package c8y;

public class OpenWeather {

	public String city;
	public double humidity;
	public double pressure;
	public double speed;
	public double temperatureMin;
	public double temperatureMax;

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public double getHumidity() {
		return humidity;
	}

	public void setHumidity(double humidity) {
		this.humidity = humidity;
	}

	public double getPressure() {
		return pressure;
	}

	public void setPressure(double pressure) {
		this.pressure = pressure;
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public double getTemperatureMin() {
		return temperatureMin;
	}

	public void setTemperatureMin(double temperatureMin) {
		this.temperatureMin = temperatureMin;
	}

	public double getTemperatureMax() {
		return temperatureMax;
	}

	public void setTemperatureMax(double temperatureMax) {
		this.temperatureMax = temperatureMax;
	}

}
