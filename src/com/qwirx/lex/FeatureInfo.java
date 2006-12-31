/*
 * Created on 24-Dec-2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.lex;

public final class FeatureInfo {
	public final String objectType, featureName, featureType;
	public FeatureInfo(String objectType, 
			String featureName, 
			String featureType) {
		this.objectType  = objectType;
		this.featureName = featureName;
		this.featureType = featureType;
	}
}