package fr.asterox.RewardsCentral.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.asterox.RewardsCentral.controller.UserManagementController;
import fr.asterox.RewardsCentral.dto.LocationDTO;
import fr.asterox.RewardsCentral.dto.UserReward;
import fr.asterox.RewardsCentral.dto.VisitedLocationDTO;
import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import rewardCentral.RewardCentral;

@Service
public class RewardsCentralService implements IRewardsCentralService {
	@Autowired
	private GpsUtil gpsUtil;
	@Autowired
	private RewardCentral rewardsCentral;
	@Autowired
	UserManagementController userManagementController;

	private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
	private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;

	public RewardsCentralService() {
		super();
	}

	public RewardsCentralService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}

	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}

	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}

	@Override
	public void calculateRewards(List<VisitedLocationDTO> userLocations, List<UserReward> userRewards,
			String userName) {
		List<Attraction> attractions = gpsUtil.getAttractions();

		for (VisitedLocationDTO visitedLocation : userLocations) {
			for (Attraction attraction : attractions) {
				if (userRewards.stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName))
						.count() == 0) {
					if (nearAttraction(visitedLocation, attraction)) {
						userManagementController.addUserReward(userName, new UserReward(visitedLocation, attraction,
								getRewardPoints(attraction, userManagementController.getUserId(userName))));
					}
				}
			}
		}
	}

	private boolean nearAttraction(VisitedLocationDTO visitedLocation, Attraction attraction) {
		return getDistance(visitedLocation.location, attraction) > proximityBuffer ? false : true;
	}

	private int getRewardPoints(Attraction attraction, UUID userId) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, userId);
	}

	public double getDistance(LocationDTO loc1, Location loc2) {
		double lat1 = Math.toRadians(loc1.latitude);
		double lon1 = Math.toRadians(loc1.longitude);
		double lat2 = Math.toRadians(loc2.latitude);
		double lon2 = Math.toRadians(loc2.longitude);

		double angle = Math
				.acos(Math.sin(lat1) * Math.sin(lat2) + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

		double nauticalMiles = 60 * Math.toDegrees(angle);
		double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
		return statuteMiles;
	}

}
