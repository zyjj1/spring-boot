/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.properties.migrator;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.IterableConfigurationPropertySource;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Report on {@link PropertyMigration properties migration}.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 */
class PropertiesMigrationReporter {

	private final Map<String, ConfigurationMetadataProperty> allProperties;

	private final ConfigurableEnvironment environment;

	PropertiesMigrationReporter(ConfigurationMetadataRepository metadataRepository,
			ConfigurableEnvironment environment) {
		this.allProperties = Collections.unmodifiableMap(metadataRepository.getAllProperties());
		this.environment = environment;
	}

	/**
	 * Analyse the {@link ConfigurableEnvironment environment} and attempt to rename
	 * legacy properties if a replacement exists.
	 * @return a report of the migration
	 */
	PropertiesMigrationReport getReport() {
		PropertiesMigrationReport report = new PropertiesMigrationReport();
		Map<String, List<PropertyMigration>> properties = getMatchingProperties(
				ConfigurationMetadataProperty::isDeprecated);
		if (properties.isEmpty()) {
			return report;
		}
		properties.forEach((name, candidates) -> {
			PropertySource<?> propertySource = mapPropertiesWithReplacement(report, name, candidates);
			if (propertySource != null) {
				this.environment.getPropertySources().addBefore(name, propertySource);
			}
		});
		return report;
	}

	private PropertySource<?> mapPropertiesWithReplacement(PropertiesMigrationReport report, String name,
			List<PropertyMigration> properties) {
		report.add(name, properties);
		List<PropertyMigration> renamed = properties.stream().filter(PropertyMigration::isCompatibleType).toList();
		if (renamed.isEmpty()) {
			return null;
		}
		NameTrackingPropertySource nameTrackingPropertySource = new NameTrackingPropertySource();
		this.environment.getPropertySources().addFirst(nameTrackingPropertySource);
		try {
			String target = "migrate-" + name;
			Map<String, OriginTrackedValue> content = new LinkedHashMap<>();
			for (PropertyMigration candidate : renamed) {
				String newPropertyName = candidate.getNewPropertyName();
				Object value = candidate.getProperty().getValue();
				if (nameTrackingPropertySource.isPlaceholderThatAccessesName(value, newPropertyName)) {
					continue;
				}
				OriginTrackedValue originTrackedValue = OriginTrackedValue.of(value,
						candidate.getProperty().getOrigin());
				content.put(newPropertyName, originTrackedValue);
			}
			return new OriginTrackedMapPropertySource(target, content);
		}
		finally {
			this.environment.getPropertySources().remove(nameTrackingPropertySource.getName());
		}
	}

	private boolean isMapType(ConfigurationMetadataProperty property) {
		String type = property.getType();
		return type != null && type.startsWith(Map.class.getName());
	}

	private Map<String, List<PropertyMigration>> getMatchingProperties(
			Predicate<ConfigurationMetadataProperty> filter) {
		MultiValueMap<String, PropertyMigration> result = new LinkedMultiValueMap<>();
		List<ConfigurationMetadataProperty> candidates = this.allProperties.values().stream().filter(filter).toList();
		getPropertySourcesAsMap().forEach((propertySourceName, propertySource) -> candidates.forEach((metadata) -> {
			ConfigurationPropertyName metadataName = ConfigurationPropertyName.isValid(metadata.getId())
					? ConfigurationPropertyName.of(metadata.getId())
					: ConfigurationPropertyName.adapt(metadata.getId(), '.');
			// Direct match
			ConfigurationProperty match = propertySource.getConfigurationProperty(metadataName);
			if (match != null) {
				result.add(propertySourceName,
						new PropertyMigration(match, metadata, determineReplacementMetadata(metadata), false));
			}
			// Prefix match for maps
			if (isMapType(metadata) && propertySource instanceof IterableConfigurationPropertySource iterableSource) {
				iterableSource.stream()
					.filter(metadataName::isAncestorOf)
					.map(propertySource::getConfigurationProperty)
					.forEach((property) -> {
						ConfigurationMetadataProperty replacement = determineReplacementMetadata(metadata);
						result.add(propertySourceName, new PropertyMigration(property, metadata, replacement, true));
					});
			}
		}));
		return result;
	}

	private ConfigurationMetadataProperty determineReplacementMetadata(ConfigurationMetadataProperty metadata) {
		String replacementId = metadata.getDeprecation().getReplacement();
		if (StringUtils.hasText(replacementId)) {
			ConfigurationMetadataProperty replacement = this.allProperties.get(replacementId);
			if (replacement != null) {
				return replacement;
			}
			return detectMapValueReplacement(replacementId);
		}
		return null;
	}

	private ConfigurationMetadataProperty detectMapValueReplacement(String fullId) {
		int lastDot = fullId.lastIndexOf('.');
		if (lastDot == -1) {
			return null;
		}
		ConfigurationMetadataProperty metadata = this.allProperties.get(fullId.substring(0, lastDot));
		if (metadata != null && isMapType(metadata)) {
			return metadata;
		}
		return null;
	}

	private Map<String, ConfigurationPropertySource> getPropertySourcesAsMap() {
		Map<String, ConfigurationPropertySource> map = new LinkedHashMap<>();
		for (ConfigurationPropertySource source : ConfigurationPropertySources.get(this.environment)) {
			map.put(determinePropertySourceName(source), source);
		}
		return map;
	}

	private String determinePropertySourceName(ConfigurationPropertySource source) {
		if (source.getUnderlyingSource() instanceof PropertySource) {
			return ((PropertySource<?>) source.getUnderlyingSource()).getName();
		}
		return source.getUnderlyingSource().toString();
	}

	/**
	 * {@link PropertySource} used to track accessed properties to protect against
	 * circular references.
	 */
	private class NameTrackingPropertySource extends PropertySource<Object> {

		private final Set<String> accessedNames = new HashSet<>();

		NameTrackingPropertySource() {
			super(NameTrackingPropertySource.class.getName());
		}

		boolean isPlaceholderThatAccessesName(Object value, String name) {
			if (value instanceof String) {
				this.accessedNames.clear();
				PropertiesMigrationReporter.this.environment.resolvePlaceholders((String) value);
				return this.accessedNames.contains(name);
			}
			return false;
		}

		@Override
		public Object getProperty(String name) {
			this.accessedNames.add(name);
			return null;
		}

	}

}
