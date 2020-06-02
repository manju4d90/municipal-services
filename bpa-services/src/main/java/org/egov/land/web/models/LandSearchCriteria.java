package org.egov.land.web.models;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandSearchCriteria {

	@JsonProperty("tenantId")
	@NotNull
	private String tenantId;

	@JsonProperty("ids")
	private List<String> ids;

	@JsonProperty("landUid")
	private String landUid;

	@JsonProperty("mobileNumber")
	private String mobileNumber;
	
    @JsonProperty("offset")
    private Integer offset;

    @JsonProperty("limit")
    private Integer limit;

	public boolean isEmpty() {
		return (this.tenantId == null && this.ids == null && this.landUid == null && this.mobileNumber == null);
	}

	public boolean tenantIdOnly() {
		return (this.tenantId != null && this.ids == null && this.landUid == null && this.mobileNumber == null);
	}
}