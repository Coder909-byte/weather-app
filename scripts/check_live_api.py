#!/usr/bin/env python3
"""Exercise the same public endpoints and query fields as the Android app. No API key required."""
import json
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from zoneinfo import ZoneInfo


def get(base, params):
    url = base + "?" + urllib.parse.urlencode(params)
    with urllib.request.urlopen(url, timeout=30) as response:
        return json.load(response)


def check_city(name, country_code):
    locations = get("https://geocoding-api.open-meteo.com/v1/search", {
        "name": name, "count": 10, "language": "en", "format": "json",
    })["results"]
    city = next(item for item in locations if item.get("country_code") == country_code)
    data = get("https://api.open-meteo.com/v1/forecast", {
        "latitude": city["latitude"], "longitude": city["longitude"],
        "current": "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m,wind_direction_10m",
        "hourly": "temperature_2m,precipitation_probability,weather_code,relative_humidity_2m,is_day",
        "daily": "temperature_2m_max,temperature_2m_min,weather_code,sunrise,sunset,uv_index_max,precipitation_probability_max",
        "timezone": "auto", "forecast_days": 7, "temperature_unit": "celsius",
        "wind_speed_unit": "kmh", "timeformat": "unixtime",
    })
    assert isinstance(data["current"]["time"], int)
    assert isinstance(data["current"]["temperature_2m"], (int, float))
    assert len(data["daily"]["time"]) == 7
    assert len(data["hourly"]["time"]) >= 24
    assert len(data["hourly"]["time"]) == len(data["hourly"]["is_day"])
    assert data["current_units"]["wind_speed_10m"] == "km/h"
    local = datetime.fromtimestamp(data["current"]["time"], ZoneInfo(data["timezone"]))
    return {
        "checked_at_utc": datetime.now(timezone.utc).isoformat(),
        "city": city["name"], "country": city["country"], "timezone": data["timezone"],
        "observed_local": local.isoformat(), "temperature_c": data["current"]["temperature_2m"],
        "weather_code": data["current"]["weather_code"],
        "hourly_rows": len(data["hourly"]["time"]), "daily_rows": len(data["daily"]["time"]),
        "status": "PASS",
    }


def main():
    checks = [check_city(name, code) for name, code in [("Amritsar", "IN"), ("London", "GB"), ("Tokyo", "JP")]]
    empty = get("https://geocoding-api.open-meteo.com/v1/search", {"name": "zzzyxqvunlikelycity", "count": 10})
    assert not empty.get("results")
    print(json.dumps({"cities": checks, "empty_search": "PASS"}, indent=2))


if __name__ == "__main__":
    main()
