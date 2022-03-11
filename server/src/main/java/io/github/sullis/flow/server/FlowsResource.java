package io.github.sullis.flow.server;

import org.openapitools.model.FlowLog;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;

import static io.github.sullis.flow.server.Utils.buildFlowLog;

@Path("/flows")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FlowsResource {
    private final FlowAggregator aggregator;

    public FlowsResource(final FlowAggregator aggregator) {
        this.aggregator = aggregator;
    }

    @GET
    public List<FlowLog> get(@QueryParam("hour") @Min(0) @Max(23) int hour) {
        Map<LookupKey, FlowTotal> data = aggregator.findByHour(hour);
        return data.entrySet().stream()
                .map((entry) -> buildFlowLog(entry.getKey(), entry.getValue()))
                .toList();
    }

    @POST
    public void post(@Valid List<FlowLog> flowLogs) {
        aggregator.record(flowLogs);
    }
}
