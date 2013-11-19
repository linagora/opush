/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2014  Linagora
 *
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU Affero General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version, provided you comply 
 * with the Additional Terms applicable for OBM connector by Linagora 
 * pursuant to Section 7 of the GNU Affero General Public License, 
 * subsections (b), (c), and (e), pursuant to which you must notably (i) retain 
 * the “Message sent thanks to OBM, Free Communication by Linagora” 
 * signature notice appended to any and all outbound messages 
 * (notably e-mail and meeting requests), (ii) retain all hypertext links between 
 * OBM and obm.org, as well as between Linagora and linagora.com, and (iii) refrain 
 * from infringing Linagora intellectual property rights over its trademarks 
 * and commercial brands. Other Additional Terms apply, 
 * see <http://www.linagora.com/licenses/> for more details. 
 *
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details. 
 *
 * You should have received a copy of the GNU Affero General Public License 
 * and its applicable Additional Terms for OBM along with this program. If not, 
 * see <http://www.gnu.org/licenses/> for the GNU Affero General Public License version 3 
 * and <http://www.linagora.com/licenses/> for the Additional Terms applicable to 
 * OBM connectors. 
 * 
 * ***** END LICENSE BLOCK ***** */
package org.obm.push.bean;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

@JsonDeserialize(builder=SyncCollectionOptions.Builder.class)
public class SyncCollectionOptions implements Serializable {
	
	private static final long serialVersionUID = 7306997586579565585L;
	public static final Integer SYNC_TRUNCATION_ALL = 9;
	
	public static SyncCollectionOptions defaultOptions() {
		return SyncCollectionOptions.builder().build();
	}
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		private Integer truncation;
		private Integer mimeSupport;
		private Integer mimeTruncation;
		private Integer conflict;
		private Boolean deletesAsMoves;
		private FilterType filterType;
		private List<BodyPreference> bodyPreferences;
		
		private Builder() {}
		
		public Builder truncation(Integer truncation) {
			this.truncation = truncation;
			return this;
		}
		
		public Builder mimeSupport(Integer mimeSupport) {
			this.mimeSupport = mimeSupport;
			return this;
		}
		
		public Builder mimeTruncation(Integer mimeTruncation) {
			this.mimeTruncation = mimeTruncation;
			return this;
		}
		
		public Builder conflict(Integer conflict) {
			this.conflict = conflict;
			return this;
		}
		
		public Builder deletesAsMoves(Boolean deletesAsMoves) {
			this.deletesAsMoves = deletesAsMoves;
			return this;
		}
		
		public Builder filterType(FilterType filterType) {
			this.filterType = filterType;
			return this;
		}
		
		public Builder bodyPreferences(List<BodyPreference> bodyPreferences) {
			this.bodyPreferences = bodyPreferences;
			return this;
		}
		
		public SyncCollectionOptions build() {
			return new SyncCollectionOptions(
					Objects.firstNonNull(truncation, SYNC_TRUNCATION_ALL),
					mimeSupport, mimeTruncation, 
					Objects.firstNonNull(conflict, 1),
					Objects.firstNonNull(deletesAsMoves, true), 
					Objects.firstNonNull(filterType, FilterType.THREE_DAYS_BACK), 
					Objects.firstNonNull(bodyPreferences, ImmutableList.<BodyPreference>of()));
		}
	}
	
	private final Integer truncation;
	private final Integer mimeSupport;
	private final Integer mimeTruncation;
	private final Integer conflict;
	private final Boolean deletesAsMoves;
	private final FilterType filterType;
	private final List<BodyPreference> bodyPreferences;
	
	private SyncCollectionOptions(Integer truncation, Integer mimeSupport, Integer mimeTruncation, Integer conflict, Boolean deletesAsMoves, FilterType filterType, List<BodyPreference> bodyPreferences) {
		this.truncation = truncation;
		this.mimeSupport = mimeSupport;
		this.mimeTruncation = mimeTruncation;
		this.conflict = conflict;
		this.deletesAsMoves = deletesAsMoves;
		this.filterType = filterType;
		this.bodyPreferences = bodyPreferences;
	}
	
	public Integer getConflict() {
		return conflict;
	}

	public Integer getTruncation() {
		return truncation;
	}

	public Boolean isDeletesAsMoves() {
		return deletesAsMoves;
	}

	public FilterType getFilterType() {
		return filterType;
	}

	public Integer getMimeSupport() {
		return mimeSupport;
	}

	public Integer getMimeTruncation() {
		return mimeTruncation;
	}

	public List<BodyPreference> getBodyPreferences() {
		return bodyPreferences;
	}

	public static SyncCollectionOptions cloneOnlyByExistingFields(SyncCollectionOptions cloningFromOptions) {
		SyncCollectionOptions.Builder builder = SyncCollectionOptions.builder();
		if (cloningFromOptions.getConflict() != null) {
			builder.conflict(cloningFromOptions.getConflict());
		}
		if (cloningFromOptions.getFilterType() != null) {
			builder.filterType(cloningFromOptions.getFilterType());
		}
		if (cloningFromOptions.getMimeSupport() != null) {
			builder.mimeSupport(cloningFromOptions.getMimeSupport());
		}
		if (cloningFromOptions.getMimeTruncation() != null) {
			builder.mimeTruncation(cloningFromOptions.getMimeTruncation());
		}
		if (cloningFromOptions.getTruncation() != null) {
			builder.truncation(cloningFromOptions.getTruncation());
		}
		if (cloningFromOptions.getBodyPreferences() != null) {
			builder.bodyPreferences(cloningFromOptions.getBodyPreferences());
		}
		return builder.build();
	}
	
	@Override
	public final int hashCode(){
		return Objects.hashCode(truncation, mimeSupport, mimeTruncation, conflict, 
				deletesAsMoves, filterType, bodyPreferences);
	}
	
	@Override
	public final boolean equals(Object object){
		if (object instanceof SyncCollectionOptions) {
			SyncCollectionOptions that = (SyncCollectionOptions) object;
			return Objects.equal(this.truncation, that.truncation)
				&& Objects.equal(this.mimeSupport, that.mimeSupport)
				&& Objects.equal(this.mimeTruncation, that.mimeTruncation)
				&& Objects.equal(this.conflict, that.conflict)
				&& Objects.equal(this.deletesAsMoves, that.deletesAsMoves)
				&& Objects.equal(this.filterType, that.filterType)
				&& Objects.equal(this.bodyPreferences, that.bodyPreferences);
		}
		return false;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("SYNC_TRUNCATION_ALL", SYNC_TRUNCATION_ALL)
			.add("truncation", truncation)
			.add("mimeSupport", mimeSupport)
			.add("mimeTruncation", mimeTruncation)
			.add("conflict", conflict)
			.add("deletesAsMoves", deletesAsMoves)
			.add("filterType", filterType)
			.add("bodyPreferences", bodyPreferences)
			.toString();
	}
}