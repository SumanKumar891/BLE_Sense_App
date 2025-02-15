package com.example.blegame;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000H\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0010!\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\f\u0012\b\u0012\u00060\u0002R\u00020\u00000\u0001:\u0001\u0019B\'\u0012\f\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u0012\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\b0\u0007\u00a2\u0006\u0002\u0010\tJ\b\u0010\u000e\u001a\u00020\rH\u0016J\u001c\u0010\u000f\u001a\u00020\b2\n\u0010\u0010\u001a\u00060\u0002R\u00020\u00002\u0006\u0010\u0011\u001a\u00020\rH\u0016J*\u0010\u000f\u001a\u00020\b2\n\u0010\u0010\u001a\u00060\u0002R\u00020\u00002\u0006\u0010\u0011\u001a\u00020\r2\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00140\u0013H\u0016J\u001c\u0010\u0015\u001a\u00060\u0002R\u00020\u00002\u0006\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\rH\u0016R\u001a\u0010\n\u001a\u000e\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\r0\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\b0\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001a"}, d2 = {"Lcom/example/blegame/GameDeviceAdapter;", "Landroidx/recyclerview/widget/RecyclerView$Adapter;", "Lcom/example/blegame/GameDeviceAdapter$BLEDeviceViewHolder;", "devices", "", "Lcom/example/blegame/BLEDevice;", "onClick", "Lkotlin/Function1;", "", "(Ljava/util/List;Lkotlin/jvm/functions/Function1;)V", "characterImages", "", "", "", "getItemCount", "onBindViewHolder", "holder", "position", "payloads", "", "", "onCreateViewHolder", "parent", "Landroid/view/ViewGroup;", "viewType", "BLEDeviceViewHolder", "app_debug"})
public final class GameDeviceAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<com.example.blegame.GameDeviceAdapter.BLEDeviceViewHolder> {
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.example.blegame.BLEDevice> devices = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function1<com.example.blegame.BLEDevice, kotlin.Unit> onClick = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.Integer> characterImages = null;
    
    public GameDeviceAdapter(@org.jetbrains.annotations.NotNull()
    java.util.List<com.example.blegame.BLEDevice> devices, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.blegame.BLEDevice, kotlin.Unit> onClick) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public com.example.blegame.GameDeviceAdapter.BLEDeviceViewHolder onCreateViewHolder(@org.jetbrains.annotations.NotNull()
    android.view.ViewGroup parent, int viewType) {
        return null;
    }
    
    @java.lang.Override()
    public void onBindViewHolder(@org.jetbrains.annotations.NotNull()
    com.example.blegame.GameDeviceAdapter.BLEDeviceViewHolder holder, int position, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.Object> payloads) {
    }
    
    @java.lang.Override()
    public void onBindViewHolder(@org.jetbrains.annotations.NotNull()
    com.example.blegame.GameDeviceAdapter.BLEDeviceViewHolder holder, int position) {
    }
    
    @java.lang.Override()
    public int getItemCount() {
        return 0;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0086\u0004\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u0012R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0011\u0010\t\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u000e\u0010\f\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0011\u0010\r\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000b\u00a8\u0006\u0013"}, d2 = {"Lcom/example/blegame/GameDeviceAdapter$BLEDeviceViewHolder;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "itemView", "Landroid/view/View;", "(Lcom/example/blegame/GameDeviceAdapter;Landroid/view/View;)V", "addressTextView", "Landroid/widget/TextView;", "characterImageView", "Landroid/widget/ImageView;", "foundLabel", "getFoundLabel", "()Landroid/widget/TextView;", "nameTextView", "rssiTextView", "getRssiTextView", "bind", "", "device", "Lcom/example/blegame/BLEDevice;", "app_debug"})
    public final class BLEDeviceViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        @org.jetbrains.annotations.NotNull()
        private final android.widget.TextView nameTextView = null;
        @org.jetbrains.annotations.NotNull()
        private final android.widget.TextView addressTextView = null;
        @org.jetbrains.annotations.NotNull()
        private final android.widget.TextView rssiTextView = null;
        @org.jetbrains.annotations.NotNull()
        private final android.widget.ImageView characterImageView = null;
        @org.jetbrains.annotations.NotNull()
        private final android.widget.TextView foundLabel = null;
        
        public BLEDeviceViewHolder(@org.jetbrains.annotations.NotNull()
        android.view.View itemView) {
            super(null);
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.widget.TextView getRssiTextView() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.widget.TextView getFoundLabel() {
            return null;
        }
        
        public final void bind(@org.jetbrains.annotations.NotNull()
        com.example.blegame.BLEDevice device) {
        }
    }
}