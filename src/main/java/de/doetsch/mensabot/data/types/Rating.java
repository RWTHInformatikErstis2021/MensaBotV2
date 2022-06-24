package de.doetsch.mensabot.data.types;

public class Rating {
	private final long userId;
	private final String city;
	private final String meal;
	private final int rating;
	public Rating(Long userId, String city, String meal, Integer rating){
		this.userId = userId;
		this.city = city;
		this.meal = meal;
		this.rating = rating;
	}
	public long getUserId(){return userId;}
	public String getCity(){return city;}
	public String getMeal(){return meal;}
	public int getRating(){return rating;}
}
