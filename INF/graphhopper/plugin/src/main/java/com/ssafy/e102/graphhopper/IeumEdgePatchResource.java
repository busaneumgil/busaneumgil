package com.ssafy.e102.graphhopper;

import com.graphhopper.routing.ev.EnumEncodedValue;
import com.graphhopper.routing.ev.IntEncodedValue;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.AllEdgesIterator;
import com.graphhopper.storage.BaseGraph;
import com.ssafy.e102.graphhopper.ieum.IeumEncodedValues;
import com.ssafy.e102.graphhopper.ieum.IeumEnum.YesNoUnknown;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/ieum/admin/edges")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class IeumEdgePatchResource {
    private static final Logger log = LoggerFactory.getLogger(IeumEdgePatchResource.class);

    private final BaseGraph baseGraph;
    private final IntEncodedValue dbEdgeIdEncodedValue;
    private final EnumEncodedValue<YesNoUnknown> walkAccessEncodedValue;

    @Inject
    public IeumEdgePatchResource(BaseGraph baseGraph, EncodingManager encodingManager) {
        this.baseGraph = baseGraph;
        this.dbEdgeIdEncodedValue = encodingManager.getIntEncodedValue(IeumEncodedValues.DB_EDGE_ID);
        this.walkAccessEncodedValue =
            encodingManager.getEnumEncodedValue(IeumEncodedValues.WALK_ACCESS, YesNoUnknown.class);
    }

    @PATCH
    @Path("/{dbEdgeId}/walk-access")
    public WalkAccessPatchResponse patchWalkAccess(
        @PathParam("dbEdgeId") long dbEdgeId,
        WalkAccessPatchRequest request) {
        if (request == null || request.walkAccess() == null || request.walkAccess().isBlank()) {
            throw new BadRequestException("walkAccess is required");
        }

        int encodedDbEdgeId = toEncodedDbEdgeId(dbEdgeId);
        YesNoUnknown nextWalkAccess = parseWalkAccess(request.walkAccess());
        int patchedEdgeCount = patchWalkAccessInternal(encodedDbEdgeId, nextWalkAccess);
        if (patchedEdgeCount == 0) {
            throw new NotFoundException("No GraphHopper edge matches dbEdgeId=" + dbEdgeId);
        }

        log.info(
            "graphhopper walk_access hot patch applied dbEdgeId={} walkAccess={} patchedEdges={}",
            dbEdgeId,
            nextWalkAccess,
            patchedEdgeCount);
        return new WalkAccessPatchResponse(dbEdgeId, nextWalkAccess.name(), patchedEdgeCount);
    }

    private int patchWalkAccessInternal(int dbEdgeId, YesNoUnknown nextWalkAccess) {
        int patchedEdgeCount = 0;
        synchronized (baseGraph) {
            AllEdgesIterator edgeIterator = baseGraph.getAllEdges();
            while (edgeIterator.next()) {
                if (edgeIterator.get(dbEdgeIdEncodedValue) != dbEdgeId) {
                    continue;
                }
                edgeIterator.set(walkAccessEncodedValue, nextWalkAccess);
                if (walkAccessEncodedValue.isStoreTwoDirections()) {
                    edgeIterator.setReverse(walkAccessEncodedValue, nextWalkAccess);
                }
                patchedEdgeCount += 1;
            }
        }
        return patchedEdgeCount;
    }

    private int toEncodedDbEdgeId(long dbEdgeId) {
        if (dbEdgeId <= 0 || dbEdgeId > Integer.MAX_VALUE) {
            throw new BadRequestException("dbEdgeId out of supported range");
        }
        return (int)dbEdgeId;
    }

    private YesNoUnknown parseWalkAccess(String walkAccess) {
        try {
            return YesNoUnknown.valueOf(walkAccess.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            throw new BadRequestException("Unsupported walkAccess value: " + walkAccess);
        }
    }

    public record WalkAccessPatchRequest(String walkAccess) {
    }

    public record WalkAccessPatchResponse(long dbEdgeId, String walkAccess, int patchedEdgeCount) {
    }
}
