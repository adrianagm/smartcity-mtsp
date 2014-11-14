package com.emergya.mtsp.webapp.dto;

import com.emergya.mtsp.model.Stop;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author lroman
 */
public class MTSPRequest implements Serializable {

    private List<Stop> origins;
    
    private List<Stop> stops;

    /**
     * @return the stops
     */
    public List<Stop> getStops() {
        return stops;
    }

    /**
     * @param stops the stops to set
     */
    public void setStops(List<Stop> stops) {
        this.stops = stops;
    }

    /**
     * @return the origin
     */
    public List<Stop> getOrigins() {
        return origins;
    }

    /**
     * @param origin the origin to set
     */
    public void setOrigins(List<Stop> origins) {
        this.origins = origins;
    }
}
