/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.loader.launch;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;

import org.springframework.boot.loader.net.protocol.jar.JarUrl;
import org.springframework.boot.loader.zip.AssertFileChannelDataBlocksClosed;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.test.tools.SourceFile;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.function.ThrowingConsumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JarLauncher}.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Phillip Webb
 */
@AssertFileChannelDataBlocksClosed
class JarLauncherTests extends AbstractExecutableArchiveLauncherTests {

	@Test
	void explodedJarHasOnlyBootInfClassesAndContentsOfBootInfLibOnClasspath() throws Exception {
		File explodedRoot = explode(createJarArchive("archive.jar", "BOOT-INF"));
		JarLauncher launcher = new JarLauncher(new ExplodedArchive(explodedRoot));
		Set<URL> urls = launcher.getClassPathUrls();
		assertThat(urls).containsExactlyInAnyOrder(getExpectedFileUrls(explodedRoot));
	}

	@Test
	void archivedJarHasOnlyBootInfClassesAndContentsOfBootInfLibOnClasspath() throws Exception {
		File jarRoot = createJarArchive("archive.jar", "BOOT-INF");
		try (JarFileArchive archive = new JarFileArchive(jarRoot)) {
			JarLauncher launcher = new JarLauncher(archive);
			Set<URL> urls = launcher.getClassPathUrls();
			List<URL> expectedUrls = new ArrayList<>();
			expectedUrls.add(JarUrl.create(jarRoot, "BOOT-INF/classes/"));
			expectedUrls.add(JarUrl.create(jarRoot, "BOOT-INF/lib/foo.jar"));
			expectedUrls.add(JarUrl.create(jarRoot, "BOOT-INF/lib/bar.jar"));
			expectedUrls.add(JarUrl.create(jarRoot, "BOOT-INF/lib/baz.jar"));
			assertThat(urls).containsOnlyOnceElementsOf(expectedUrls);
		}
	}

	@Test
	void explodedJarShouldPreserveClasspathOrderWhenIndexPresent() throws Exception {
		File explodedRoot = explode(createJarArchive("archive.jar", "BOOT-INF", true, Collections.emptyList()));
		JarLauncher launcher = new JarLauncher(new ExplodedArchive(explodedRoot));
		URLClassLoader classLoader = createClassLoader(launcher);
		assertThat(classLoader.getURLs()).containsExactly(getExpectedFileUrls(explodedRoot));
	}

	@Test
	void jarFilesPresentInBootInfLibsAndNotInClasspathIndexShouldBeAddedAfterBootInfClasses() throws Exception {
		ArrayList<String> extraLibs = new ArrayList<>(Arrays.asList("extra-1.jar", "extra-2.jar"));
		File explodedRoot = explode(createJarArchive("archive.jar", "BOOT-INF", true, extraLibs));
		JarLauncher launcher = new JarLauncher(new ExplodedArchive(explodedRoot));
		URLClassLoader classLoader = createClassLoader(launcher);
		List<File> expectedFiles = getExpectedFilesWithExtraLibs(explodedRoot);
		URL[] expectedFileUrls = expectedFiles.stream().map(this::toUrl).toArray(URL[]::new);
		assertThat(classLoader.getURLs()).containsExactly(expectedFileUrls);
	}

	@Test
	void explodedJarDefinedPackagesIncludeManifestAttributes() {
		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		attributes.put(Name.MANIFEST_VERSION, "1.0");
		attributes.put(Name.IMPLEMENTATION_TITLE, "test");
		SourceFile sourceFile = SourceFile.of("explodedsample/ExampleClass.java",
				new ClassPathResource("explodedsample/ExampleClass.txt"));
		TestCompiler.forSystem().compile(sourceFile, ThrowingConsumer.of((compiled) -> {
			File explodedRoot = explode(
					createJarArchive("archive.jar", manifest, "BOOT-INF", true, Collections.emptyList()));
			File target = new File(explodedRoot, "BOOT-INF/classes/explodedsample/ExampleClass.class");
			target.getParentFile().mkdirs();
			FileCopyUtils.copy(compiled.getClassLoader().getResourceAsStream("explodedsample/ExampleClass.class"),
					new FileOutputStream(target));
			JarLauncher launcher = new JarLauncher(new ExplodedArchive(explodedRoot));
			URLClassLoader classLoader = createClassLoader(launcher);
			Class<?> loaded = classLoader.loadClass("explodedsample.ExampleClass");
			assertThat(loaded.getPackage().getImplementationTitle()).isEqualTo("test");
		}));
	}

	private URLClassLoader createClassLoader(JarLauncher launcher) throws Exception {
		return (URLClassLoader) launcher.createClassLoader(launcher.getClassPathUrls());
	}

	private URL[] getExpectedFileUrls(File explodedRoot) {
		return getExpectedFiles(explodedRoot).stream().map(this::toUrl).toArray(URL[]::new);
	}

	private List<File> getExpectedFiles(File parent) {
		List<File> expected = new ArrayList<>();
		expected.add(new File(parent, "BOOT-INF/classes"));
		expected.add(new File(parent, "BOOT-INF/lib/foo.jar"));
		expected.add(new File(parent, "BOOT-INF/lib/bar.jar"));
		expected.add(new File(parent, "BOOT-INF/lib/baz.jar"));
		return expected;
	}

	private List<File> getExpectedFilesWithExtraLibs(File parent) {
		List<File> expected = new ArrayList<>();
		expected.add(new File(parent, "BOOT-INF/classes"));
		expected.add(new File(parent, "BOOT-INF/lib/extra-1.jar"));
		expected.add(new File(parent, "BOOT-INF/lib/extra-2.jar"));
		expected.add(new File(parent, "BOOT-INF/lib/foo.jar"));
		expected.add(new File(parent, "BOOT-INF/lib/bar.jar"));
		expected.add(new File(parent, "BOOT-INF/lib/baz.jar"));
		return expected;
	}

}
