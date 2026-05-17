package com.ssafy.e102.graphhopper;

import com.graphhopper.config.Profile;
import com.graphhopper.routing.DefaultWeightingFactory;
import com.graphhopper.routing.WeightingFactory;
import com.graphhopper.routing.ev.IntEncodedValue;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.util.PMap;
import com.ssafy.e102.graphhopper.ieum.IeumEncodedValues;

final class OverlayAwareWeightingFactory implements WeightingFactory {

	private final WeightingFactory delegateFactory;
	private final IntEncodedValue dbEdgeIdEncodedValue;
	private final RoutingSegmentOverrideStore routingSegmentOverrideStore;

	OverlayAwareWeightingFactory(
		BaseGraph baseGraph,
		EncodingManager encodingManager,
		RoutingSegmentOverrideStore routingSegmentOverrideStore) {
		this.delegateFactory = new DefaultWeightingFactory(baseGraph, encodingManager);
		this.dbEdgeIdEncodedValue = encodingManager.getIntEncodedValue(IeumEncodedValues.DB_EDGE_ID);
		this.routingSegmentOverrideStore = routingSegmentOverrideStore;
	}

	@Override
	public Weighting createWeighting(Profile profile, PMap hints, boolean disableTurnCosts) {
		Weighting delegate = delegateFactory.createWeighting(profile, hints, disableTurnCosts);
		return new OverlayAwareWeighting(delegate, dbEdgeIdEncodedValue, routingSegmentOverrideStore);
	}
}
