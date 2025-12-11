package net.luversof.web.dynamiccrud.setting.domain;

import java.time.ZonedDateTime;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class Setting {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long idx;

	@Column(length = 40)
	private String writer;

	@CreationTimestamp
	private ZonedDateTime createDate;

	@UpdateTimestamp
	private ZonedDateTime updateDate;

	public long getIdx() {
		return idx;
	}

	public void setIdx(long idx) {
		this.idx = idx;
	}

	public String getWriter() {
		return writer;
	}

	public void setWriter(String writer) {
		this.writer = writer;
	}

	public ZonedDateTime getCreateDate() {
		return createDate;
	}

	public void setCreateDate(ZonedDateTime createDate) {
		this.createDate = createDate;
	}

	public ZonedDateTime getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(ZonedDateTime updateDate) {
		this.updateDate = updateDate;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Setting other = (Setting) obj;
		return Objects.equals(createDate, other.createDate) && idx == other.idx
				&& Objects.equals(updateDate, other.updateDate) && Objects.equals(writer, other.writer);
	}

	@Override
	public int hashCode() {
		return Objects.hash(createDate, idx, updateDate, writer);
	}

	@Override
	public String toString() {
		return "Setting [idx=" + idx + ", writer=" + writer + ", createDate=" + createDate + ", updateDate="
				+ updateDate + "]";
	}

}
