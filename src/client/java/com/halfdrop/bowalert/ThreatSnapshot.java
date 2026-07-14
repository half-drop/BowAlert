package com.halfdrop.bowalert;

record ThreatSnapshot(Threat primary, int count) {
    static final ThreatSnapshot EMPTY = new ThreatSnapshot(null, 0);
}
