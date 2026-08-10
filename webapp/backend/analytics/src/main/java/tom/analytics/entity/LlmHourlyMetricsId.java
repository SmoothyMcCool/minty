package tom.analytics.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class LlmHourlyMetricsId implements Serializable {
	private static final long serialVersionUID = -722145193556234318L;

	@Column(name = "day")
	private LocalDate day;

	@Column(name = "hour")
	private Integer hour;

	public LlmHourlyMetricsId() {
	}

	public LlmHourlyMetricsId(LocalDate day, Integer hour) {
		this.day = day;
		this.hour = hour;
	}

	public LocalDate getDay() {
		return day;
	}

	public void setDay(LocalDate day) {
		this.day = day;
	}

	public Integer getHour() {
		return hour;
	}

	public void setHour(Integer hour) {
		this.hour = hour;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!(object instanceof LlmHourlyMetricsId other)) {
			return false;
		}

		return Objects.equals(day, other.day) && Objects.equals(hour, other.hour);
	}

	@Override
	public int hashCode() {
		return Objects.hash(day, hour);
	}
}
