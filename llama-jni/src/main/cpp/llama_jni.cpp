#include <jni.h>
#include <string>
#include <android/log.h>
#include <vector>
#include <atomic>
#include "llama.h"

#define TAG "LlamaJni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,     TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,    TAG, __VA_ARGS__)

std::atomic<bool> is_interrupted(false);

extern "C" JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* reserved) {
    LOGI("JNI_OnLoad: initializing llama backend");
    llama_backend_init();
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM* vm, void* reserved) {
    LOGI("JNI_OnUnload: freeing llama backend");
    llama_backend_free();
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_mahavtaar_vibecoder_llm_LlamaJni_loadModel(
        JNIEnv* env, jobject obj, jstring modelPath, jint contextSize, jint nGpuLayers) {

    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    if (!path) {
        LOGE("Failed to get model path from Java string");
        return 0;
    }

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = nGpuLayers;

    LOGI("Loading model from %s with %d GPU layers", path, nGpuLayers);
    llama_model* model = llama_load_model_from_file(path, model_params);
    env->ReleaseStringUTFChars(modelPath, path);

    if (!model) {
        LOGE("Failed to load model from %s", path);
        return 0;
    }

    LOGI("Model loaded successfully");
    return reinterpret_cast<jlong>(model);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_mahavtaar_vibecoder_llm_LlamaJni_createContext(
        JNIEnv* env, jobject obj, jlong modelPtr, jint contextSize, jint nThreads) {

    llama_model* model = reinterpret_cast<llama_model*>(modelPtr);
    if (!model) {
        LOGE("Invalid model pointer in createContext");
        return 0;
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = contextSize;
    ctx_params.n_threads = nThreads;
    ctx_params.n_threads_batch = nThreads;

    LOGI("Creating context with size %d, threads %d", contextSize, nThreads);
    llama_context* ctx = llama_new_context_with_model(model, ctx_params);
    if (!ctx) {
        LOGE("Failed to create context");
        return 0;
    }

    return reinterpret_cast<jlong>(ctx);
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_mahavtaar_vibecoder_llm_LlamaJni_tokenize(
        JNIEnv* env, jobject obj, jlong ctxPtr, jstring text, jboolean addBos) {

    llama_context* ctx = reinterpret_cast<llama_context*>(ctxPtr);
    if (!ctx) return nullptr;

    llama_model* model = (llama_model*)llama_get_model(ctx);

    const char* str = env->GetStringUTFChars(text, nullptr);
    if (!str) return nullptr;

    std::string text_str(str);
    env->ReleaseStringUTFChars(text, str);

    // Initial allocation for tokens, n_vocab is a safe upper bound or text length + 1.
    int max_tokens = text_str.length() + (addBos ? 1 : 0);
    std::vector<llama_token> tokens(max_tokens);

    // parse_special is true (1) by default for chat models
    int n_tokens = llama_tokenize(model, text_str.c_str(), text_str.length(), tokens.data(), max_tokens, addBos, true);

    if (n_tokens < 0) {
        LOGE("Failed to tokenize string. Need %d tokens but allocated %d", -n_tokens, max_tokens);
        // Resize and try again
        tokens.resize(-n_tokens);
        n_tokens = llama_tokenize(model, text_str.c_str(), text_str.length(), tokens.data(), tokens.size(), addBos, true);
        if (n_tokens < 0) {
            LOGE("Failed to tokenize string twice.");
            return nullptr;
        }
    }

    jintArray result = env->NewIntArray(n_tokens);
    env->SetIntArrayRegion(result, 0, n_tokens, reinterpret_cast<jint*>(tokens.data()));

    return result;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mahavtaar_vibecoder_llm_LlamaJni_generate(
        JNIEnv* env, jobject obj, jlong ctxPtr, jintArray tokenIds, jint maxNewTokens, jfloat temperature, jfloat topP, jobject callback) {

    llama_context* ctx = reinterpret_cast<llama_context*>(ctxPtr);
    if (!ctx) return env->NewStringUTF("");

    llama_model* model = (llama_model*)llama_get_model(ctx);

    jsize len = env->GetArrayLength(tokenIds);
    jint* elements = env->GetIntArrayElements(tokenIds, nullptr);
    std::vector<llama_token> prompt_tokens(elements, elements + len);
    env->ReleaseIntArrayElements(tokenIds, elements, JNI_ABORT);

    // Eval the prompt
    llama_batch batch = llama_batch_init(512, 0, 1);

    for (size_t i = 0; i < prompt_tokens.size(); ++i) {
        batch.token[batch.n_tokens] = prompt_tokens[i];
        batch.pos[batch.n_tokens]   = i;
        batch.n_seq_id[batch.n_tokens] = 1;
        batch.seq_id[batch.n_tokens][0] = 0;
        batch.logits[batch.n_tokens] = false;
        batch.n_tokens++;
    }
    // Only logits for the last token in the prompt
    batch.logits[batch.n_tokens - 1] = true;

    if (llama_decode(ctx, batch) != 0) {
        LOGE("llama_decode() failed on prompt");
        llama_batch_free(batch);
        return env->NewStringUTF("");
    }

    jclass callbackClass = env->GetObjectClass(callback);
    jmethodID onTokenMethod = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)V");

    is_interrupted.store(false);

    // Setup sampler
    struct llama_sampler * smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(topP, 1));
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(temperature));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    int n_cur = prompt_tokens.size();
    int n_decode = 0;
    std::string full_response = "";

    while (n_decode < maxNewTokens && !is_interrupted.load()) {
        llama_token new_token_id = llama_sampler_sample(smpl, ctx, -1);

        // Accept the token
        llama_sampler_accept(smpl, new_token_id);

        if (llama_token_is_eog(model, new_token_id)) {
            break;
        }

        char buf[256];
        int n = llama_token_to_piece(model, new_token_id, buf, sizeof(buf), 0, true);
        if (n < 0) {
            LOGE("Failed to decode token");
            break;
        }
        std::string token_str(buf, n);
        full_response += token_str;

        // Callback
        jstring jToken = env->NewStringUTF(token_str.c_str());
        env->CallVoidMethod(callback, onTokenMethod, jToken);
        env->DeleteLocalRef(jToken);

        // Prepare next batch
        batch.n_tokens = 0;
        batch.token[0] = new_token_id;
        batch.pos[0] = n_cur;
        batch.n_seq_id[0] = 1;
        batch.seq_id[0][0] = 0;
        batch.logits[0] = true;
        batch.n_tokens++;

        if (llama_decode(ctx, batch) != 0) {
            LOGE("llama_decode() failed during generation");
            break;
        }

        n_cur += 1;
        n_decode += 1;
    }

    llama_sampler_free(smpl);
    llama_batch_free(batch);

    return env->NewStringUTF(full_response.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_mahavtaar_vibecoder_llm_LlamaJni_stopGeneration(JNIEnv* env, jobject obj) {
    is_interrupted.store(true);
}

extern "C" JNIEXPORT void JNICALL
Java_com_mahavtaar_vibecoder_llm_LlamaJni_freeContext(JNIEnv* env, jobject obj, jlong ctxPtr) {
    llama_context* ctx = reinterpret_cast<llama_context*>(ctxPtr);
    if (ctx) {
        LOGI("Freeing context");
        llama_free(ctx);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_mahavtaar_vibecoder_llm_LlamaJni_freeModel(JNIEnv* env, jobject obj, jlong modelPtr) {
    llama_model* model = reinterpret_cast<llama_model*>(modelPtr);
    if (model) {
        LOGI("Freeing model");
        llama_free_model(model);
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mahavtaar_vibecoder_llm_LlamaJni_getModelInfo(JNIEnv* env, jobject obj, jlong modelPtr) {
    llama_model* model = reinterpret_cast<llama_model*>(modelPtr);
    if (!model) return env->NewStringUTF("{}");

    char buf[1024];
    int n_ctx_train = llama_model_n_ctx_train(model);
    int n_vocab = llama_model_n_vocab(model);
    uint64_t n_params = llama_model_n_params(model);

    snprintf(buf, sizeof(buf), "{\"n_params\":%llu,\"n_ctx_train\":%d,\"n_vocab\":%d}",
             (unsigned long long)n_params, n_ctx_train, n_vocab);

    return env->NewStringUTF(buf);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_mahavtaar_vibecoder_llm_LlamaJni_getBatchSize(JNIEnv* env, jobject obj) {
    return 512;
}
