package io.github.sullis.netflix.server;

import org.openapitools.model.FlowLog;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.core.MediaType;

import java.util.Collections;
import java.util.List;

@Path("/flows")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FlowsResource {
    private final FlowAggregator aggregator;

    public FlowsResource(final FlowAggregator aggregator) {
        this.aggregator = aggregator;
    }

    @GET
    public List<FlowLog> get(@QueryParam("hour") int hour) {
        // TODO return aggregator.findByHour(hour);
        return Collections.emptyList();
    }

    @POST
    public List<FlowLog> post(@Valid List<FlowLog> flowLogs) {
        aggregator.record(flowLogs);
        return Collections.emptyList();
    }
}
