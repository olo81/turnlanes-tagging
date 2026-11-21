// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.turnlanestagging.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author ruben
 */
public class BLanes implements Serializable {

    String type;
    List<BLane> lanes = new ArrayList<>();

    public BLanes() {
    }

    public BLanes(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
        changeDirectionLanes(type);

    }

    public List<BLane> getLanes() {
        return lanes;
    }

    public void setLanes(List<BLane> newLanes) {

        Map<Integer, BLane> map = new TreeMap<>();
        for (int i = 0; i < newLanes.size(); i++) {
            map.put(newLanes.get(i).getPosition(), newLanes.get(i));
        }
        List<BLane> listLs = new ArrayList<>(map.values());
        this.lanes = listLs;

    }

    public String getTagturns() {
        String tagturns = "";
        for (int i = 0; i < lanes.size(); i++) {
            if (i == 0) {
                tagturns = lanes.get(i).getTurn();
            } else {
                tagturns = tagturns + "|" + lanes.get(i).getTurn();
            }
        }
        return tagturns;
    }

    public String getTagchanges() {
        String tagchanges = "";
        for (int i = 0; i < lanes.size(); i++) {
            if (i == 0) {
                tagchanges = lanes.get(i).getChange();
            } else {
                tagchanges = tagchanges + "|" + lanes.get(i).getChange();
            }
        }
        return tagchanges;
    }

    /**
     * Builds the {@code bicycle:lanes} value for this block: {@code designated}
     * for bicycle lanes, {@code no} where bicycles are forbidden and
     * {@code yes} for the remaining lanes.
     */
    public String getTagbicycle() {
        String tagbicycle = "";
        for (int i = 0; i < lanes.size(); i++) {
            String value = lanes.get(i).getBicycle();
            if (value.isEmpty()) {
                value = "yes";
            }
            tagbicycle = i == 0 ? value : tagbicycle + "|" + value;
        }
        return tagbicycle;
    }

    /**
     * Builds the {@code vehicle:lanes} value for this block.
     */
    public String getTagvehicle() {
        String tagvehicle = "";
        for (int i = 0; i < lanes.size(); i++) {
            String value = "no".equals(lanes.get(i).getVehicle()) ? "no" : "yes";
            tagvehicle = i == 0 ? value : tagvehicle + "|" + value;
        }
        return tagvehicle;
    }

    /**
     * Builds the {@code cycleway:lanes} value for this block: {@code lane}
     * where the lane is a designated bicycle lane and {@code no} elsewhere.
     */
    public String getTagcycleway() {
        String tagcycleway = "";
        for (int i = 0; i < lanes.size(); i++) {
            String value = lanes.get(i).isBike() ? "lane" : "no";
            tagcycleway = i == 0 ? value : tagcycleway + "|" + value;
        }
        return tagcycleway;
    }

    /**
     * Whether any lane in this block is a designated bicycle lane.
     */
    public boolean hasBike() {
        for (BLane lane : lanes) {
            if (lane.isBike()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether any lane in this block forbids motor vehicles.
     */
    public boolean hasVehicleNo() {
        for (BLane lane : lanes) {
            if ("no".equals(lane.getVehicle())) {
                return true;
            }
        }
        return false;
    }

    public void setStringLanes(String type, String turnLanes) {
        applyLaneValues(type, turnLanes.split("\\|", -1), null, null, null);
    }

    public void setStringChanges(String type, String changeLanes) {
        applyLaneValues(type, null, changeLanes.split("\\|", -1), null, null);
    }

    public void setStringBicycle(String type, String bicycleLanes) {
        applyLaneValues(type, null, null, bicycleLanes.split("\\|", -1), null);
    }

    public void setStringVehicle(String type, String vehicleLanes) {
        applyLaneValues(type, null, null, null, vehicleLanes.split("\\|", -1));
    }

    /**
     * Applies {@code cycleway:lanes} values: a lane with {@code lane} or
     * {@code track} becomes a designated bicycle lane, other values leave the
     * existing {@code bicycle:lanes} value untouched.
     */
    public void setStringCycleway(String type, String cyclewayLanes) {
        String[] values = cyclewayLanes.split("\\|", -1);
        String[] merged = new String[Math.max(values.length, lanes.size())];
        for (int i = 0; i < merged.length; i++) {
            String existingBicycle = i < lanes.size() ? lanes.get(i).getBicycle() : "";
            boolean isLane = i < values.length && ("lane".equals(values[i]) || "track".equals(values[i]));
            merged[i] = isLane ? "designated" : existingBicycle;
        }
        applyLaneValues(type, null, null, merged, null);
    }

    /**
     * Builds/updates the lane list by position, preserving the turn value while
     * setting changes, bicycle or vehicle values (or vice versa). This keeps
     * the several {@code :lanes} tags in sync regardless of the order they are
     * processed in.
     */
    private void applyLaneValues(String type, String[] turns, String[] changes, String[] bicycles, String[] vehicles) {
        Map<Integer, BLane> existing = new TreeMap<>();
        for (BLane lane : lanes) {
            existing.put(lane.getPosition(), lane);
        }

        int count = lanes.size();
        if (turns != null) {
            count = Math.max(count, turns.length);
        }
        if (changes != null) {
            count = Math.max(count, changes.length);
        }
        if (bicycles != null) {
            count = Math.max(count, bicycles.length);
        }
        if (vehicles != null) {
            count = Math.max(count, vehicles.length);
        }

        List<BLane> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int position = i + 1;
            BLane previous = existing.get(position);
            String turn = turns != null ? normalizeTurn(turns[i]) : (previous != null ? previous.getTurn() : "");
            String change = changes != null ? changes[i] : (previous != null ? previous.getChange() : "");
            String bicycle = bicycles != null ? normalizeBicycle(bicycles[i])
                    : (previous != null ? previous.getBicycle() : "");
            String vehicle = vehicles != null ? normalizeVehicle(vehicles[i])
                    : (previous != null ? previous.getVehicle() : "");
            BLane lane = new BLane(type, position, turn, change);
            lane.setBicycle(bicycle);
            lane.setVehicle(vehicle);
            result.add(lane);
        }
        this.lanes = result;
    }

    private String normalizeBicycle(String value) {
        if ("designated".equals(value) || "no".equals(value)) {
            return value;
        }
        return "";
    }

    private String normalizeVehicle(String value) {
        return "no".equals(value) ? "no" : "";
    }

    private String normalizeTurn(String turn) {
        if (turn.equals("none")) {
            return "";
        }
        if (turn.indexOf(";") > 0) {
            return sortTurns(turn);
        }
        return turn;
    }

    private String sortTurns(String roadTurns) {
        //https://wiki.openstreetmap.org/wiki/Key:turn
        List<String> turnsList = Arrays.asList(
                "reverse", "sharp_left", "left", "slight_left", "merge_to_right", "through",
                "reversible", "merge_to_left", "slight_right", "right", "sharp_right");
        List<String> roadTurnsList = Arrays.asList(roadTurns.split(";"));
        List<String> newRoadTurns = new ArrayList<>();
        for (String e : turnsList) {
            if (roadTurnsList.indexOf(e) > -1) {
                newRoadTurns.add(e);
            }
        }
        return newRoadTurns.toString().replaceAll("\\[|\\]", "").replaceAll(", ", ";");
    }

    public void changeDirectionLanes(String type) {
        for (int k = 0; k < lanes.size(); k++) {
            lanes.get(k).setType(type);
        }
    }
}
