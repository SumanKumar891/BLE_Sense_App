package com.example.blegame;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000h\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010#\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010%\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\"\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0011\n\u0000\n\u0002\u0010\u0015\n\u0002\b\b\u0018\u0000 (2\u00020\u0001:\u0001(B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0017\u001a\u00020\u0013H\u0002J\u0012\u0010\u0018\u001a\u00020\u00192\b\u0010\u001a\u001a\u0004\u0018\u00010\u001bH\u0014J-\u0010\u001c\u001a\u00020\u00192\u0006\u0010\u001d\u001a\u00020\u00112\u000e\u0010\u001e\u001a\n\u0012\u0006\b\u0001\u0012\u00020\u000b0\u001f2\u0006\u0010 \u001a\u00020!H\u0016\u00a2\u0006\u0002\u0010\"J\b\u0010#\u001a\u00020\u0019H\u0002J\u0010\u0010$\u001a\u00020\u00192\u0006\u0010%\u001a\u00020\u000bH\u0002J\b\u0010&\u001a\u00020\u0019H\u0002J\b\u0010\'\u001a\u00020\u0019H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000R\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u000e0\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u00110\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\u0013X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0014\u001a\u0004\u0018\u00010\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0016X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006)"}, d2 = {"Lcom/example/blegame/MultiplayerActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "bleAdapter", "Lcom/example/blegame/BluetoothAdapterWrapper;", "bleScanCallback", "Landroid/bluetooth/le/ScanCallback;", "deviceAdapter", "Lcom/example/blegame/GameDeviceAdapter;", "deviceAddresses", "", "", "deviceList", "", "Lcom/example/blegame/BLEDevice;", "deviceRSSI", "", "", "isScanning", "", "selectedDevice", "targetDevices", "", "hasPermissions", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "onRequestPermissionsResult", "requestCode", "permissions", "", "grantResults", "", "(I[Ljava/lang/String;[I)V", "refreshBLEScan", "showToast", "message", "startBLEScan", "stopBLEScan", "Companion", "app_release"})
public final class MultiplayerActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.example.blegame.BluetoothAdapterWrapper bleAdapter;
    private com.example.blegame.GameDeviceAdapter deviceAdapter;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.example.blegame.BLEDevice> deviceList = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.Integer> deviceRSSI = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Set<java.lang.String> deviceAddresses = null;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String selectedDevice;
    private boolean isScanning = false;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Set<java.lang.String> targetDevices = null;
    @org.jetbrains.annotations.NotNull()
    private final android.bluetooth.le.ScanCallback bleScanCallback = null;
    private static final int REQUEST_CODE_PERMISSIONS = 1002;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.blegame.MultiplayerActivity.Companion Companion = null;
    
    public MultiplayerActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void refreshBLEScan() {
    }
    
    private final void startBLEScan() {
    }
    
    private final void stopBLEScan() {
    }
    
    private final boolean hasPermissions() {
        return false;
    }
    
    private final void showToast(java.lang.String message) {
    }
    
    @java.lang.Override()
    public void onRequestPermissionsResult(int requestCode, @org.jetbrains.annotations.NotNull()
    java.lang.String[] permissions, @org.jetbrains.annotations.NotNull()
    int[] grantResults) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/example/blegame/MultiplayerActivity$Companion;", "", "()V", "REQUEST_CODE_PERMISSIONS", "", "app_release"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}