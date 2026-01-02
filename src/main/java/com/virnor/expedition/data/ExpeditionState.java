package com.virnor.expedition.data;

public enum ExpeditionState {
    READY,          // Chest hazır, oyuncu yaklaşabilir
    ACTIVE,         // Moblar spawn olmuş, savaş devam ediyor
    CONQUERED,      // Moblar öldürüldü, sahibi var
    COOLDOWN        // Loot alındı, cooldown'da
}
