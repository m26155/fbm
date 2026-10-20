package com.example.fbm;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.fbm.databinding.FragmentFirstBinding;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private static final String PREFS_NAME = "FBM_Prefs";
    private static final String KEY_RPI_IP = "rpi_ip";
    private static final String KEY_RPI_PORT = "rpi_port";

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedIp = prefs.getString(KEY_RPI_IP, "");
        String savedPort = prefs.getString(KEY_RPI_PORT, "5000");
        binding.edittextRpiIp.setText(savedIp);
        binding.edittextRpiPort.setText(savedPort);

        binding.buttonSaveConfig.setOnClickListener(v -> {
            String ip = binding.edittextRpiIp.getText().toString().trim();
            String port = binding.edittextRpiPort.getText().toString().trim();
            prefs.edit()
                    .putString(KEY_RPI_IP, ip)
                    .putString(KEY_RPI_PORT, port)
                    .apply();
            Toast.makeText(requireContext(), R.string.config_saved, Toast.LENGTH_SHORT).show();
        });

        binding.buttonTestSend.setOnClickListener(v -> {
            android.util.Log.d("FirstFragment", "Test Send button clicked");
            String ip = binding.edittextRpiIp != null && binding.edittextRpiIp.getText() != null 
                    ? binding.edittextRpiIp.getText().toString().trim() : "";
            String port = binding.edittextRpiPort != null && binding.edittextRpiPort.getText() != null 
                    ? binding.edittextRpiPort.getText().toString().trim() : "5000";
            
            android.util.Log.d("FirstFragment", "IP: " + ip + ", Port: " + port);
            if (ip.isEmpty()) {
                Toast.makeText(requireContext(), "IPアドレスを入力してください", Toast.LENGTH_SHORT).show();
                return;
            }
            String url = "http://" + ip + ":" + (port.isEmpty() ? "5000" : port) + "/notify";
            Toast.makeText(requireContext(), "ラズパイへテスト送信中...", Toast.LENGTH_SHORT).show();
            
            NetworkClient.sendNotification(url, "com.google.android.gm", "テスト通知タイトル", "これはラズパイとの通信テストメッセージです", new NetworkClient.NetworkCallback() {
                @Override
                public void onSuccess(String response) {
                    android.util.Log.d("FirstFragment", "Test send success: " + response);
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> 
                            Toast.makeText(requireContext(), "送信成功！レスポンス: " + response, Toast.LENGTH_LONG).show()
                        );
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    android.util.Log.e("FirstFragment", "Test send failure", e);
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> 
                            Toast.makeText(requireContext(), "送信失敗: " + e.getMessage(), Toast.LENGTH_LONG).show()
                        );
                    }
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}