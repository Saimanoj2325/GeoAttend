package com.geoattend.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class FaceRecognitionProcessor {
    private static final String MODEL_FILE = "mobilefacenet.tflite";
    private static final int INPUT_SIZE = 160;
    private final Interpreter interpreter;
    private final ImageProcessor imageProcessor;

    public FaceRecognitionProcessor(Context context) throws IOException {
        interpreter = new Interpreter(loadModelFile(context));
        imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(127.5f, 128.0f))
                .build();
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        try (AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_FILE);
             FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor())) {
            return inputStream.getChannel().map(FileChannel.MapMode.READ_ONLY, 
                fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());
        }
    }

    public float[] getEmbedding(Bitmap bitmap, Rect boundingBox) {
        int left = Math.max(0, boundingBox.left);
        int top = Math.max(0, boundingBox.top);
        int width = Math.min(bitmap.getWidth() - left, boundingBox.width());
        int height = Math.min(bitmap.getHeight() - top, boundingBox.height());
        
        if (width <= 0 || height <= 0) return null;

        Bitmap faceBitmap = Bitmap.createBitmap(bitmap, left, top, width, height);

        TensorImage tensorImage = new TensorImage(org.tensorflow.lite.DataType.FLOAT32);
        tensorImage.load(faceBitmap);
        TensorImage processedImage = imageProcessor.process(tensorImage);

        float[][] output = new float[1][512];
        interpreter.run(processedImage.getBuffer(), output);
        return output[0];
    }

    public static double calculateDistance(float[] e1, float[] e2) {
        if (e1 == null || e2 == null || e1.length != e2.length) return Double.MAX_VALUE;
        double sum = 0;
        for (int i = 0; i < e1.length; i++) {
            sum += Math.pow(e1[i] - e2[i], 2);
        }
        return Math.sqrt(sum);
    }

    public static List<Double> floatArrayToList(float[] array) {
        List<Double> list = new ArrayList<>();
        for (float f : array) list.add((double) f);
        return list;
    }

    public void close() {
        if (interpreter != null) interpreter.close();
    }
}
