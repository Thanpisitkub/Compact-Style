package com.blackbone.state;

/** Which active skill state a player is currently in. */
public enum BlackBoneMode {
	NONE(0),
	FLOW(1),
	ZONE(2);

	public final int id;

	BlackBoneMode(int id) {
		this.id = id;
	}

	public static BlackBoneMode byId(int id) {
		for (BlackBoneMode mode : values()) {
			if (mode.id == id) {
				return mode;
			}
		}

		return NONE;
	}
}
