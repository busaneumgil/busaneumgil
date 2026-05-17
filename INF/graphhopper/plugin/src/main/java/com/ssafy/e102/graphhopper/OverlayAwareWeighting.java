package com.ssafy.e102.graphhopper;

import com.graphhopper.routing.ev.IntEncodedValue;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.EdgeIteratorState;
import com.ssafy.e102.graphhopper.ieum.IeumEnum.YesNoUnknown;

final class OverlayAwareWeighting implements Weighting {

	private final Weighting delegate;
	private final IntEncodedValue dbEdgeIdEncodedValue;
	private final RoutingSegmentOverrideStore routingSegmentOverrideStore;

	OverlayAwareWeighting(
		Weighting delegate,
		IntEncodedValue dbEdgeIdEncodedValue,
		RoutingSegmentOverrideStore routingSegmentOverrideStore) {
		this.delegate = delegate;
		this.dbEdgeIdEncodedValue = dbEdgeIdEncodedValue;
		this.routingSegmentOverrideStore = routingSegmentOverrideStore;
	}

	@Override
	public double calcMinWeightPerDistance() {
		return delegate.calcMinWeightPerDistance();
	}

	@Override
	public double calcEdgeWeight(EdgeIteratorState edgeState, boolean reverse) {
		if (isBlocked(edgeState)) {
			return Double.POSITIVE_INFINITY;
		}
		return delegate.calcEdgeWeight(edgeState, reverse);
	}

	@Override
	public long calcEdgeMillis(EdgeIteratorState edgeState, boolean reverse) {
		if (isBlocked(edgeState)) {
			return Long.MAX_VALUE;
		}
		return delegate.calcEdgeMillis(edgeState, reverse);
	}

	@Override
	public double calcTurnWeight(int inEdge, int viaNode, int outEdge) {
		return delegate.calcTurnWeight(inEdge, viaNode, outEdge);
	}

	@Override
	public long calcTurnMillis(int inEdge, int viaNode, int outEdge) {
		return delegate.calcTurnMillis(inEdge, viaNode, outEdge);
	}

	@Override
	public boolean hasTurnCosts() {
		return delegate.hasTurnCosts();
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	private boolean isBlocked(EdgeIteratorState edgeState) {
		int dbEdgeId = edgeState.get(dbEdgeIdEncodedValue);
		YesNoUnknown overrideState = routingSegmentOverrideStore.getWalkAccessOverride(dbEdgeId);
		return overrideState == YesNoUnknown.NO;
	}
}
