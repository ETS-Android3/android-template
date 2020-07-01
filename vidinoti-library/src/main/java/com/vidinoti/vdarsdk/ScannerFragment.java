package com.vidinoti.vdarsdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.vidinoti.android.vdarsdk.VDARAnnotationView;
import com.vidinoti.android.vdarsdk.VDARCode;
import com.vidinoti.android.vdarsdk.VDARContext;
import com.vidinoti.android.vdarsdk.VDARPrior;
import com.vidinoti.android.vdarsdk.VDARRemoteController;
import com.vidinoti.android.vdarsdk.VDARRemoteControllerListener;
import com.vidinoti.android.vdarsdk.VDARSDKController;
import com.vidinoti.android.vdarsdk.VDARSDKControllerEventReceiver;
import com.vidinoti.android.vdarsdk.camera.DeviceCameraImageSender;

import java.io.IOException;
import java.util.ArrayList;


public class ScannerFragment extends Fragment implements VDARRemoteControllerListener, VDARSDKControllerEventReceiver {

    private static final String TAG = ScannerFragment.class.getName();

    private static final String QR_CODE_TAG_PREFIX = "pixliveplayer/";

    private ScannerFragmentListener listener;
    private VDARAnnotationView annotationView;
    private ProgressBar progressBar;
    private TextView progressTextView;
    private View overlayView;

    public ScannerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            new DeviceCameraImageSender();
        } catch (IOException e) {
            Log.e(TAG, "Error initializing camera");
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ScannerFragmentListener) {
            listener = (ScannerFragmentListener) context;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.vidinoti_scanner_fragment, container, false);
        annotationView = view.findViewById(R.id.annotationView);
        annotationView.setDarkScreenMode(false);
        annotationView.setAnimationSpeed(0);

        progressBar = view.findViewById(R.id.progressBar);
        progressBar.setMax(100);
        progressBar.setIndeterminate(false);
        progressTextView = view.findViewById(R.id.progressTextView);
        overlayView = view.findViewById(R.id.overlayView);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        annotationView.onResume();
        VDARRemoteController.getInstance().addProgressListener(this);
        VDARSDKController.getInstance().registerEventReceiver(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        annotationView.onPause();
        hideSynchronizationProgress();
        VDARRemoteController.getInstance().removeProgressListener(this);
        VDARSDKController.getInstance().unregisterEventReceiver(this);
    }

    private void hideSynchronizationProgress() {
        progressTextView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        overlayView.setVisibility(View.VISIBLE);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onSyncProgress(VDARRemoteController vdarRemoteController, float progress, boolean ready, String folder) {
        if (progress < 100) {
            progressTextView.setText((int) progress + "%");
            progressTextView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            overlayView.setVisibility(View.GONE);
        } else {
            hideSynchronizationProgress();
        }
    }

    @Override
    public void onCodesRecognized(ArrayList<VDARCode> arrayList) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        VidinotiUtils.vibrate(context);
        if (arrayList != null && !arrayList.isEmpty()) {
            String code = arrayList.get(0).getCodeData();
            if (code == null) {
                return;
            }
            if (code.startsWith(QR_CODE_TAG_PREFIX)) {
                VidinotiAR.getInstance().onTagQrCodeScanned(code.substring(QR_CODE_TAG_PREFIX.length()));
            } else if (code.startsWith("http://") || code.startsWith("https://")) {
                VidinotiUtils.openUrlInBrowser(getContext(), code);
            }
        }
    }

    @Override
    public void onFatalError(String s) {
        Log.e(TAG, "Vidinoti SDK fatal error: " + s);
    }

    @Override
    public void onPresentAnnotations() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    overlayView.setVisibility(View.GONE);
                }
            });
        }
        if (listener != null) {
            listener.onPresentAnnotations();
        }
    }

    @Override
    public void onAnnotationsHidden() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    overlayView.setVisibility(View.VISIBLE);
                }
            });
        }
        if (listener != null) {
            listener.onAnnotationsHidden();
        }
    }

    @Override
    public void onTrackingStarted(int i, int i1) {

    }

    @Override
    public void onEnterContext(VDARContext vdarContext) {

    }

    @Override
    public void onExitContext(VDARContext vdarContext) {

    }

    @Override
    public void onRequireSynchronization(ArrayList<VDARPrior> arrayList) {
        Log.d(TAG, "Vidinoti SDK onRequireSynchronization - not implemented");
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) overlayView.getLayoutParams();
        int horizontal = getResources().getDimensionPixelSize(R.dimen.vidinoti_scanner_overlay_horizontal_margin);
        int vertical = getResources().getDimensionPixelSize(R.dimen.vidinoti_scanner_overlay_vertical_margin);
        params.setMargins(horizontal, vertical, horizontal, vertical);
        overlayView.setLayoutParams(params);
    }
}
