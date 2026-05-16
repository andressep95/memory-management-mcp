package com.cloudcentinel.memory_management_mcp.infrastructure.embedding;

import ai.djl.ModelException;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.EmbeddingVector;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class DjlEmbeddingService implements EmbeddingService {

    private static final String MODEL_NAME = "intfloat/multilingual-e5-small";

    private volatile ZooModel<String, float[]> model;
    private final Object lock = new Object();

    @Override
    public EmbeddingVector embed(String text) {
        ZooModel<String, float[]> loadedModel = getOrLoadModel();
        try (Predictor<String, float[]> predictor = loadedModel.newPredictor()) {
            float[] vector = predictor.predict(text);
            return new EmbeddingVector(vector);
        } catch (TranslateException e) {
            throw new RuntimeException("Failed to compute embedding for text", e);
        }
    }

    @Override
    public List<EmbeddingVector> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) return List.of();
        ZooModel<String, float[]> loadedModel = getOrLoadModel();
        // batchPredict requires equal token-length tensors (StackBatchifier limitation).
        // Reusing one Predictor across sequential predict() calls avoids that constraint.
        try (Predictor<String, float[]> predictor = loadedModel.newPredictor()) {
            List<EmbeddingVector> results = new ArrayList<>(texts.size());
            for (String text : texts) {
                results.add(new EmbeddingVector(predictor.predict(text)));
            }
            return results;
        } catch (TranslateException e) {
            throw new RuntimeException("Failed to batch embed texts", e);
        }
    }

    private ZooModel<String, float[]> getOrLoadModel() {
        if (model == null) {
            synchronized (lock) {
                if (model == null) {
                    model = loadModel();
                }
            }
        }
        return model;
    }

    private ZooModel<String, float[]> loadModel() {
        try {
            Criteria<String, float[]> criteria = Criteria.builder()
                    .setTypes(String.class, float[].class)
                    .optModelUrls("djl://ai.djl.huggingface.pytorch/" + MODEL_NAME)
                    .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                    .build();
            return criteria.loadModel();
        } catch (ModelException | IOException e) {
            throw new RuntimeException("Failed to load DJL model: " + MODEL_NAME, e);
        }
    }
}
