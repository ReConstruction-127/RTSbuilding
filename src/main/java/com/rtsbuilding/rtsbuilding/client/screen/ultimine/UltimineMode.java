package com.rtsbuilding.rtsbuilding.client.screen.ultimine;

/**
 * Mining mode for the Ultimine (quick-destroy) panel.
 */
public enum UltimineMode {
    /** BFS flood-fill through connected blocks of the same type. */
    CHAIN,
    /** Breaks all breakable blocks in a volume around the target. */
    AREA
}
