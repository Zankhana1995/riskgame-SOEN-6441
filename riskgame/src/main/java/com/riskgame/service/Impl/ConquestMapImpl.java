package com.riskgame.service.Impl;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.util.StringUtils;

import com.riskgame.model.Continent;
import com.riskgame.model.RiskMap;
import com.riskgame.model.Territory;
import com.riskgame.service.ConquestMapInterface;


public class ConquestMapImpl implements ConquestMapInterface {

	public static final String MAP_DIR_PATH = "src/main/resources/maps/";

	enum FileDivider {
		MAP, CONTINENT, TERRITORY;
	}
//	public static void main(String[] args) {
//		ConquestMapImpl impl = new ConquestMapImpl();
//		impl.readMap("India.map");
//	}

	private FileDivider currentSection;
	private Map<Integer, Continent> continentMap;
	private List<Territory> territoryList;

	@Override
	public boolean saveMapToFile(RiskMap riskMap)
			throws UnsupportedEncodingException, FileNotFoundException, IOException {
		boolean result = false;

		try (PrintStream fileWriter = new PrintStream(
				new BufferedOutputStream(new FileOutputStream(MAP_DIR_PATH + riskMap.getMapName() + ".map")))) {
			fileWriter.println("[Map]");
			fileWriter.println("author=Sean O'Connor");
			fileWriter.println("image=" + riskMap.getMapName() + ".map");
			fileWriter.println("wrap=no");
			fileWriter.println("scroll=horizontal");
			fileWriter.println("warn=yes");
			fileWriter.println();

			fileWriter.println("[Continents]");

			Map<Integer, Continent> continentMap = riskMap.getContinents();

			for (Map.Entry<Integer, Continent> entry : continentMap.entrySet()) {

				Continent continent = entry.getValue();

				fileWriter.println(continent.getContinentName() + "=" + continent.getContinentValue());

			}
			fileWriter.println();
			fileWriter.println("[Territories]");
			for (Map.Entry<Integer, Continent> entry : continentMap.entrySet()) {

				Continent continent = entry.getValue();

				List<Territory> territoriList = continent.getTerritoryList();
				for (Territory territory : territoriList) {

					if (territory != null) {

						String territoryInfo = "";

						territoryInfo = StringUtils.arrayToDelimitedString(
								new String[] { territory.getTerritoryName(), String.valueOf(territory.getXAxis()),
										String.valueOf(territory.getYAxis()), continent.getContinentName() },
								",");

						List<String> adjTerritoryList = new ArrayList<>();

						List<String> neighbourList = territory.getNeighbourTerritories();
						if (neighbourList != null && !neighbourList.isEmpty()) {

							for (String neighbourName : neighbourList) {
								adjTerritoryList.add(neighbourName);
							}
							territoryInfo += "," + StringUtils.arrayToDelimitedString(adjTerritoryList.toArray(), ",");
							fileWriter.println(territoryInfo);

						}

					}
				}
				fileWriter.println();
			}

			if (fileWriter.checkError()) {
				result = false;
			} else {
				result = true;
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	@Override
	public RiskMap readMap(String fileName) {

		String mapline = "";
		RiskMap riskMap = new RiskMap();
		continentMap = new HashMap<Integer, Continent>();
		territoryList = new ArrayList<Territory>();
		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(MAP_DIR_PATH + fileName))) {
			int continentCount = 1;
			int territoryCount = 1;

			while ((mapline = bufferedReader.readLine()) != null) {
				if (mapline != null && !mapline.isEmpty()) {
					riskMap.setMapName(fileName);
					if (mapline.equalsIgnoreCase("[Map]")) {
						currentSection = FileDivider.MAP;
						mapline = bufferedReader.readLine();
					} else if (mapline.equalsIgnoreCase("[Continents]")) {
						currentSection = FileDivider.CONTINENT;
						mapline = bufferedReader.readLine();
					} else if (mapline.equalsIgnoreCase("[TERRITORIES]")) {
						currentSection = FileDivider.TERRITORY;
						mapline = bufferedReader.readLine();
					}
					if (currentSection == FileDivider.MAP)
						continue;
					if (currentSection == FileDivider.CONTINENT) {
						setContinentsToMapObject(mapline, continentCount);
						continentCount++;
					}
					if (currentSection == FileDivider.TERRITORY) {
						setTerritoriesToMapObject(mapline, territoryCount);
						territoryCount++;
					}
				}
			}
			setTerritoryListinContinent();
			riskMap.setContinents(continentMap);

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(riskMap);
		return riskMap;
	}

	private void setTerritoryListinContinent() {
		for (Entry<Integer, Continent> entry : continentMap.entrySet()) {
			Continent c = entry.getValue();
			List<Territory> continentsTerritoryList = new ArrayList<Territory>();
			for (Territory territory : territoryList) {
				if (territory.getContinentIndex() == entry.getKey()) {
					continentsTerritoryList.add(territory);
				}
			}
			c.setTerritoryList(continentsTerritoryList);
		}
	}

	private void setTerritoriesToMapObject(String mapline, int territoryCount) {
		if (mapline.length() > 0) {
			List<String> neighbourTerritories = new ArrayList<String>();
			Territory territory = new Territory();
			territory.setTerritoryIndex(territoryCount);
			String[] strArray = mapline.split(",");
			int length = strArray.length;
			territory.setTerritoryName(strArray[0]);
			territory.setXAxis(Integer.parseInt(strArray[1]));
			territory.setYAxis(Integer.parseInt(strArray[2]));
			for (Entry<Integer, Continent> entry : continentMap.entrySet()) {
				if (entry.getValue().getContinentName().equals(strArray[3])) {
					territory.setContinentIndex(entry.getKey());
				}
			}
			for (int i = 4; i < length; i++) {
				neighbourTerritories.add(strArray[i].trim());
			}
			territory.setNeighbourTerritories(neighbourTerritories);
			territoryList.add(territory);
		}
	}

	private void setContinentsToMapObject(String mapline, int continentCount) {
		if (mapline.length() > 0) {
			Continent continent = new Continent();
			String[] strArray = mapline.split("=");
			continent.setContinentName(strArray[0].trim());
			continent.setContinentValue(Integer.parseInt(strArray[1].trim()));
			continent.setContinentIndex(continentCount);
			continentMap.put(continentCount, continent);
		}
	}
}
