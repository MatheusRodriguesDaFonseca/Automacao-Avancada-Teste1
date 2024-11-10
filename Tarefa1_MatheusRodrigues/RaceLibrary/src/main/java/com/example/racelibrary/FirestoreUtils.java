package com.example.racelibrary;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class FirestoreUtils {
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();


    public static void saveCarState(CarData carData) {
        db.collection("cars").document(carData.getName()).set(carData).addOnSuccessListener(aVoid -> Log.d("Firestore", "Carro salvo com sucesso!")).addOnFailureListener(e -> Log.w("Firestore", "Erro ao salvar carro", e));
    }


    public static void loadCarStates(OnLoadCompleteListener listener) {
        db.collection("cars")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean hasCars = false;
                        for (DocumentSnapshot document : task.getResult()) {
                            CarData carData = document.toObject(CarData.class);
                            if (carData != null) {
                                listener.onLoadComplete(carData);
                                hasCars = true;
                            }
                        }
                        if (hasCars) {
                            Log.d("Firestore", "Carros carregados, prontos para iniciar.");
                        } else {
                            Log.d("Firestore", "Nenhum carro salvo encontrado.");
                        }
                    } else {
                        Log.w("Firestore", "Erro ao carregar estados dos carros", task.getException());
                    }
                });
    }

    public interface OnLoadCompleteListener {
        void onLoadComplete(CarData carData);
    }
}
