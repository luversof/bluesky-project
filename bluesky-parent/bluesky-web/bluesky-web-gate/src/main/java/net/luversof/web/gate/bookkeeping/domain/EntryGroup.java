package net.luversof.web.gate.bookkeeping.domain;

import net.luversof.web.gate.bookkeeping.constant.EntryGroupType;

public record EntryGroup(long idx, String entryGroupId, String bookkeepingId, EntryGroupType entryGroupType,
		String name) {

	public static Builder builder() {
		return new Builder();
	}

	public Builder toBuilder() {
		return new Builder(this);
	}

	public static class Builder {
		private long idx;
		private String entryGroupId;
		private String bookkeepingId;
		private EntryGroupType entryGroupType;
		private String name;

		public Builder() {
		}

		public Builder(EntryGroup entryGroup) {
			this.idx = entryGroup.idx();
			this.entryGroupId = entryGroup.entryGroupId();
			this.bookkeepingId = entryGroup.bookkeepingId();
			this.entryGroupType = entryGroup.entryGroupType();
			this.name = entryGroup.name();
		}

		public Builder idx(long idx) {
			this.idx = idx;
			return this;
		}

		public Builder entryGroupId(String entryGroupId) {
			this.entryGroupId = entryGroupId;
			return this;
		}

		public Builder bookkeepingId(String bookkeepingId) {
			this.bookkeepingId = bookkeepingId;
			return this;
		}

		public Builder entryGroupType(EntryGroupType entryGroupType) {
			this.entryGroupType = entryGroupType;
			return this;
		}

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public EntryGroup build() {
			return new EntryGroup(idx, entryGroupId, bookkeepingId, entryGroupType, name);
		}
	}
}
