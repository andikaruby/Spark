/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.spark.unsafe.types;

import org.apache.spark.SparkException;
import org.apache.spark.sql.catalyst.util.CollationFactory;
import org.apache.spark.sql.catalyst.util.CollationSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class CollationSupportSuite {

  /**
   * Collation-aware string expressions.
   */

  private void assertContains(String pattern, String target, String collationName, boolean expected)
          throws SparkException {
    UTF8String l = UTF8String.fromString(pattern);
    UTF8String r = UTF8String.fromString(target);
    int collationId = CollationFactory.collationNameToId(collationName);
    assertEquals(expected, CollationSupport.Contains.exec(l, r, collationId));
  }

  @Test
  public void testContains() throws SparkException {
    // Edge cases
    assertContains("", "", "UTF8_BINARY", true);
    assertContains("c", "", "UTF8_BINARY", true);
    assertContains("", "c", "UTF8_BINARY", false);
    assertContains("", "", "UNICODE", true);
    assertContains("c", "", "UNICODE", true);
    assertContains("", "c", "UNICODE", false);
    assertContains("", "", "UTF8_BINARY_LCASE", true);
    assertContains("c", "", "UTF8_BINARY_LCASE", true);
    assertContains("", "c", "UTF8_BINARY_LCASE", false);
    assertContains("", "", "UNICODE_CI", true);
    assertContains("c", "", "UNICODE_CI", true);
    assertContains("", "c", "UNICODE_CI", false);
    // Basic tests
    assertContains("abcde", "bcd", "UTF8_BINARY", true);
    assertContains("abcde", "bde", "UTF8_BINARY", false);
    assertContains("abcde", "fgh", "UTF8_BINARY", false);
    assertContains("abcde", "abcde", "UNICODE", true);
    assertContains("abcde", "aBcDe", "UNICODE", false);
    assertContains("abcde", "fghij", "UNICODE", false);
    assertContains("abcde", "C", "UTF8_BINARY_LCASE", true);
    assertContains("abcde", "AbCdE", "UTF8_BINARY_LCASE", true);
    assertContains("abcde", "X", "UTF8_BINARY_LCASE", false);
    assertContains("abcde", "c", "UNICODE_CI", true);
    assertContains("abcde", "bCD", "UNICODE_CI", true);
    assertContains("abcde", "123", "UNICODE_CI", false);
    // Case variation
    assertContains("aBcDe", "bcd", "UTF8_BINARY", false);
    assertContains("aBcDe", "BcD", "UTF8_BINARY", true);
    assertContains("aBcDe", "abcde", "UNICODE", false);
    assertContains("aBcDe", "aBcDe", "UNICODE", true);
    assertContains("aBcDe", "bcd", "UTF8_BINARY_LCASE", true);
    assertContains("aBcDe", "BCD", "UTF8_BINARY_LCASE", true);
    assertContains("aBcDe", "abcde", "UNICODE_CI", true);
    assertContains("aBcDe", "AbCdE", "UNICODE_CI", true);
    // Accent variation
    assertContains("aBcDe", "bćd", "UTF8_BINARY", false);
    assertContains("aBcDe", "BćD", "UTF8_BINARY", false);
    assertContains("aBcDe", "abćde", "UNICODE", false);
    assertContains("aBcDe", "aBćDe", "UNICODE", false);
    assertContains("aBcDe", "bćd", "UTF8_BINARY_LCASE", false);
    assertContains("aBcDe", "BĆD", "UTF8_BINARY_LCASE", false);
    assertContains("aBcDe", "abćde", "UNICODE_CI", false);
    assertContains("aBcDe", "AbĆdE", "UNICODE_CI", false);
    // Variable byte length characters
    assertContains("ab世De", "b世D", "UTF8_BINARY", true);
    assertContains("ab世De", "B世d", "UTF8_BINARY", false);
    assertContains("äbćδe", "bćδ", "UTF8_BINARY", true);
    assertContains("äbćδe", "BcΔ", "UTF8_BINARY", false);
    assertContains("ab世De", "ab世De", "UNICODE", true);
    assertContains("ab世De", "AB世dE", "UNICODE", false);
    assertContains("äbćδe", "äbćδe", "UNICODE", true);
    assertContains("äbćδe", "ÄBcΔÉ", "UNICODE", false);
    assertContains("ab世De", "b世D", "UTF8_BINARY_LCASE", true);
    assertContains("ab世De", "B世d", "UTF8_BINARY_LCASE", true);
    assertContains("äbćδe", "bćδ", "UTF8_BINARY_LCASE", true);
    assertContains("äbćδe", "BcΔ", "UTF8_BINARY_LCASE", false);
    assertContains("ab世De", "ab世De", "UNICODE_CI", true);
    assertContains("ab世De", "AB世dE", "UNICODE_CI", true);
    assertContains("äbćδe", "ÄbćδE", "UNICODE_CI", true);
    assertContains("äbćδe", "ÄBcΔÉ", "UNICODE_CI", false);
    // Case-variable character length
    assertContains("abİo12", "i̇o", "UNICODE_CI", true);
    assertContains("abi̇o12", "İo", "UNICODE_CI", true);
  }

  private void assertStartsWith(
          String pattern, String prefix, String collationName, boolean expected)
          throws SparkException {
    UTF8String l = UTF8String.fromString(pattern);
    UTF8String r = UTF8String.fromString(prefix);
    int collationId = CollationFactory.collationNameToId(collationName);
    assertEquals(expected, CollationSupport.StartsWith.exec(l, r, collationId));
  }

  @Test
  public void testStartsWith() throws SparkException {
    // Edge cases
    assertStartsWith("", "", "UTF8_BINARY", true);
    assertStartsWith("c", "", "UTF8_BINARY", true);
    assertStartsWith("", "c", "UTF8_BINARY", false);
    assertStartsWith("", "", "UNICODE", true);
    assertStartsWith("c", "", "UNICODE", true);
    assertStartsWith("", "c", "UNICODE", false);
    assertStartsWith("", "", "UTF8_BINARY_LCASE", true);
    assertStartsWith("c", "", "UTF8_BINARY_LCASE", true);
    assertStartsWith("", "c", "UTF8_BINARY_LCASE", false);
    assertStartsWith("", "", "UNICODE_CI", true);
    assertStartsWith("c", "", "UNICODE_CI", true);
    assertStartsWith("", "c", "UNICODE_CI", false);
    // Basic tests
    assertStartsWith("abcde", "abc", "UTF8_BINARY", true);
    assertStartsWith("abcde", "abd", "UTF8_BINARY", false);
    assertStartsWith("abcde", "fgh", "UTF8_BINARY", false);
    assertStartsWith("abcde", "abcde", "UNICODE", true);
    assertStartsWith("abcde", "aBcDe", "UNICODE", false);
    assertStartsWith("abcde", "fghij", "UNICODE", false);
    assertStartsWith("abcde", "A", "UTF8_BINARY_LCASE", true);
    assertStartsWith("abcde", "AbCdE", "UTF8_BINARY_LCASE", true);
    assertStartsWith("abcde", "X", "UTF8_BINARY_LCASE", false);
    assertStartsWith("abcde", "a", "UNICODE_CI", true);
    assertStartsWith("abcde", "aBC", "UNICODE_CI", true);
    assertStartsWith("abcde", "bcd", "UNICODE_CI", false);
    assertStartsWith("abcde", "123", "UNICODE_CI", false);
    // Case variation
    assertStartsWith("aBcDe", "abc", "UTF8_BINARY", false);
    assertStartsWith("aBcDe", "aBc", "UTF8_BINARY", true);
    assertStartsWith("aBcDe", "abcde", "UNICODE", false);
    assertStartsWith("aBcDe", "aBcDe", "UNICODE", true);
    assertStartsWith("aBcDe", "abc", "UTF8_BINARY_LCASE", true);
    assertStartsWith("aBcDe", "ABC", "UTF8_BINARY_LCASE", true);
    assertStartsWith("aBcDe", "abcde", "UNICODE_CI", true);
    assertStartsWith("aBcDe", "AbCdE", "UNICODE_CI", true);
    // Accent variation
    assertStartsWith("aBcDe", "abć", "UTF8_BINARY", false);
    assertStartsWith("aBcDe", "aBć", "UTF8_BINARY", false);
    assertStartsWith("aBcDe", "abćde", "UNICODE", false);
    assertStartsWith("aBcDe", "aBćDe", "UNICODE", false);
    assertStartsWith("aBcDe", "abć", "UTF8_BINARY_LCASE", false);
    assertStartsWith("aBcDe", "ABĆ", "UTF8_BINARY_LCASE", false);
    assertStartsWith("aBcDe", "abćde", "UNICODE_CI", false);
    assertStartsWith("aBcDe", "AbĆdE", "UNICODE_CI", false);
    // Variable byte length characters
    assertStartsWith("ab世De", "ab世", "UTF8_BINARY", true);
    assertStartsWith("ab世De", "aB世", "UTF8_BINARY", false);
    assertStartsWith("äbćδe", "äbć", "UTF8_BINARY", true);
    assertStartsWith("äbćδe", "äBc", "UTF8_BINARY", false);
    assertStartsWith("ab世De", "ab世De", "UNICODE", true);
    assertStartsWith("ab世De", "AB世dE", "UNICODE", false);
    assertStartsWith("äbćδe", "äbćδe", "UNICODE", true);
    assertStartsWith("äbćδe", "ÄBcΔÉ", "UNICODE", false);
    assertStartsWith("ab世De", "ab世", "UTF8_BINARY_LCASE", true);
    assertStartsWith("ab世De", "aB世", "UTF8_BINARY_LCASE", true);
    assertStartsWith("äbćδe", "äbć", "UTF8_BINARY_LCASE", true);
    assertStartsWith("äbćδe", "äBc", "UTF8_BINARY_LCASE", false);
    assertStartsWith("ab世De", "ab世De", "UNICODE_CI", true);
    assertStartsWith("ab世De", "AB世dE", "UNICODE_CI", true);
    assertStartsWith("äbćδe", "ÄbćδE", "UNICODE_CI", true);
    assertStartsWith("äbćδe", "ÄBcΔÉ", "UNICODE_CI", false);
    // Case-variable character length
    assertStartsWith("İonic", "i̇o", "UNICODE_CI", true);
    assertStartsWith("i̇onic", "İo", "UNICODE_CI", true);
  }

  private void assertEndsWith(String pattern, String suffix, String collationName, boolean expected)
          throws SparkException {
    UTF8String l = UTF8String.fromString(pattern);
    UTF8String r = UTF8String.fromString(suffix);
    int collationId = CollationFactory.collationNameToId(collationName);
    assertEquals(expected, CollationSupport.EndsWith.exec(l, r, collationId));
  }

  @Test
  public void testEndsWith() throws SparkException {
    // Edge cases
    assertEndsWith("", "", "UTF8_BINARY", true);
    assertEndsWith("c", "", "UTF8_BINARY", true);
    assertEndsWith("", "c", "UTF8_BINARY", false);
    assertEndsWith("", "", "UNICODE", true);
    assertEndsWith("c", "", "UNICODE", true);
    assertEndsWith("", "c", "UNICODE", false);
    assertEndsWith("", "", "UTF8_BINARY_LCASE", true);
    assertEndsWith("c", "", "UTF8_BINARY_LCASE", true);
    assertEndsWith("", "c", "UTF8_BINARY_LCASE", false);
    assertEndsWith("", "", "UNICODE_CI", true);
    assertEndsWith("c", "", "UNICODE_CI", true);
    assertEndsWith("", "c", "UNICODE_CI", false);
    // Basic tests
    assertEndsWith("abcde", "cde", "UTF8_BINARY", true);
    assertEndsWith("abcde", "bde", "UTF8_BINARY", false);
    assertEndsWith("abcde", "fgh", "UTF8_BINARY", false);
    assertEndsWith("abcde", "abcde", "UNICODE", true);
    assertEndsWith("abcde", "aBcDe", "UNICODE", false);
    assertEndsWith("abcde", "fghij", "UNICODE", false);
    assertEndsWith("abcde", "E", "UTF8_BINARY_LCASE", true);
    assertEndsWith("abcde", "AbCdE", "UTF8_BINARY_LCASE", true);
    assertEndsWith("abcde", "X", "UTF8_BINARY_LCASE", false);
    assertEndsWith("abcde", "e", "UNICODE_CI", true);
    assertEndsWith("abcde", "CDe", "UNICODE_CI", true);
    assertEndsWith("abcde", "bcd", "UNICODE_CI", false);
    assertEndsWith("abcde", "123", "UNICODE_CI", false);
    // Case variation
    assertEndsWith("aBcDe", "cde", "UTF8_BINARY", false);
    assertEndsWith("aBcDe", "cDe", "UTF8_BINARY", true);
    assertEndsWith("aBcDe", "abcde", "UNICODE", false);
    assertEndsWith("aBcDe", "aBcDe", "UNICODE", true);
    assertEndsWith("aBcDe", "cde", "UTF8_BINARY_LCASE", true);
    assertEndsWith("aBcDe", "CDE", "UTF8_BINARY_LCASE", true);
    assertEndsWith("aBcDe", "abcde", "UNICODE_CI", true);
    assertEndsWith("aBcDe", "AbCdE", "UNICODE_CI", true);
    // Accent variation
    assertEndsWith("aBcDe", "ćde", "UTF8_BINARY", false);
    assertEndsWith("aBcDe", "ćDe", "UTF8_BINARY", false);
    assertEndsWith("aBcDe", "abćde", "UNICODE", false);
    assertEndsWith("aBcDe", "aBćDe", "UNICODE", false);
    assertEndsWith("aBcDe", "ćde", "UTF8_BINARY_LCASE", false);
    assertEndsWith("aBcDe", "ĆDE", "UTF8_BINARY_LCASE", false);
    assertEndsWith("aBcDe", "abćde", "UNICODE_CI", false);
    assertEndsWith("aBcDe", "AbĆdE", "UNICODE_CI", false);
    // Variable byte length characters
    assertEndsWith("ab世De", "世De", "UTF8_BINARY", true);
    assertEndsWith("ab世De", "世dE", "UTF8_BINARY", false);
    assertEndsWith("äbćδe", "ćδe", "UTF8_BINARY", true);
    assertEndsWith("äbćδe", "cΔé", "UTF8_BINARY", false);
    assertEndsWith("ab世De", "ab世De", "UNICODE", true);
    assertEndsWith("ab世De", "AB世dE", "UNICODE", false);
    assertEndsWith("äbćδe", "äbćδe", "UNICODE", true);
    assertEndsWith("äbćδe", "ÄBcΔÉ", "UNICODE", false);
    assertEndsWith("ab世De", "世De", "UTF8_BINARY_LCASE", true);
    assertEndsWith("ab世De", "世dE", "UTF8_BINARY_LCASE", true);
    assertEndsWith("äbćδe", "ćδe", "UTF8_BINARY_LCASE", true);
    assertEndsWith("äbćδe", "cδE", "UTF8_BINARY_LCASE", false);
    assertEndsWith("ab世De", "ab世De", "UNICODE_CI", true);
    assertEndsWith("ab世De", "AB世dE", "UNICODE_CI", true);
    assertEndsWith("äbćδe", "ÄbćδE", "UNICODE_CI", true);
    assertEndsWith("äbćδe", "ÄBcΔÉ", "UNICODE_CI", false);
    // Case-variable character length
    assertEndsWith("The İo", "i̇o", "UNICODE_CI", true);
    assertEndsWith("The i̇o", "İo", "UNICODE_CI", true);
  }

  private void assertUpper(String target, String collationName, String expected)
          throws SparkException {
    UTF8String target_utf8 = UTF8String.fromString(target);
    UTF8String expected_utf8 = UTF8String.fromString(expected);
    int collationId = CollationFactory.collationNameToId(collationName);
    assertEquals(expected_utf8, CollationSupport.Upper.exec(target_utf8, collationId));
  }

  @Test
  public void testUpper() throws SparkException {
    // Edge cases
    assertUpper("", "UTF8_BINARY", "");
    assertUpper("", "UTF8_BINARY_LCASE", "");
    assertUpper("", "UNICODE", "");
    assertUpper("", "UNICODE_CI", "");
    // Basic tests
    assertUpper("abcde", "UTF8_BINARY", "ABCDE");
    assertUpper("abcde", "UTF8_BINARY_LCASE", "ABCDE");
    assertUpper("abcde", "UNICODE", "ABCDE");
    assertUpper("abcde", "UNICODE_CI", "ABCDE");
    // Uppercase present
    assertUpper("AbCdE", "UTF8_BINARY", "ABCDE");
    assertUpper("aBcDe", "UTF8_BINARY", "ABCDE");
    assertUpper("AbCdE", "UTF8_BINARY_LCASE", "ABCDE");
    assertUpper("aBcDe", "UTF8_BINARY_LCASE", "ABCDE");
    assertUpper("AbCdE", "UNICODE", "ABCDE");
    assertUpper("aBcDe", "UNICODE", "ABCDE");
    assertUpper("AbCdE", "UNICODE_CI", "ABCDE");
    assertUpper("aBcDe", "UNICODE_CI", "ABCDE");
    // Accent letters
    assertUpper("aBćDe","UTF8_BINARY", "ABĆDE");
    assertUpper("aBćDe","UTF8_BINARY_LCASE", "ABĆDE");
    assertUpper("aBćDe","UNICODE", "ABĆDE");
    assertUpper("aBćDe","UNICODE_CI", "ABĆDE");
    // Variable byte length characters
    assertUpper("ab世De", "UTF8_BINARY", "AB世DE");
    assertUpper("äbćδe", "UTF8_BINARY", "ÄBĆΔE");
    assertUpper("ab世De", "UTF8_BINARY_LCASE", "AB世DE");
    assertUpper("äbćδe", "UTF8_BINARY_LCASE", "ÄBĆΔE");
    assertUpper("ab世De", "UNICODE", "AB世DE");
    assertUpper("äbćδe", "UNICODE", "ÄBĆΔE");
    assertUpper("ab世De", "UNICODE_CI", "AB世DE");
    assertUpper("äbćδe", "UNICODE_CI", "ÄBĆΔE");
    // Case-variable character length
    assertUpper("i̇o", "UTF8_BINARY","İO");
    assertUpper("i̇o", "UTF8_BINARY_LCASE","İO");
    assertUpper("i̇o", "UNICODE","İO");
    assertUpper("i̇o", "UNICODE_CI","İO");
  }

  private void assertLower(String target, String collationName, String expected)
          throws SparkException {
    UTF8String target_utf8 = UTF8String.fromString(target);
    UTF8String expected_utf8 = UTF8String.fromString(expected);
    int collationId = CollationFactory.collationNameToId(collationName);
    assertEquals(expected_utf8, CollationSupport.Lower.exec(target_utf8, collationId));
  }

  @Test
  public void testLower() throws SparkException {
    // Edge cases
    assertLower("", "UTF8_BINARY", "");
    assertLower("", "UTF8_BINARY_LCASE", "");
    assertLower("", "UNICODE", "");
    assertLower("", "UNICODE_CI", "");
    // Basic tests
    assertLower("ABCDE", "UTF8_BINARY", "abcde");
    assertLower("ABCDE", "UTF8_BINARY_LCASE", "abcde");
    assertLower("ABCDE", "UNICODE", "abcde");
    assertLower("ABCDE", "UNICODE_CI", "abcde");
    // Uppercase present
    assertLower("AbCdE", "UTF8_BINARY", "abcde");
    assertLower("aBcDe", "UTF8_BINARY", "abcde");
    assertLower("AbCdE", "UTF8_BINARY_LCASE", "abcde");
    assertLower("aBcDe", "UTF8_BINARY_LCASE", "abcde");
    assertLower("AbCdE", "UNICODE", "abcde");
    assertLower("aBcDe", "UNICODE", "abcde");
    assertLower("AbCdE", "UNICODE_CI", "abcde");
    assertLower("aBcDe", "UNICODE_CI", "abcde");
    // Accent letters
    assertLower("AbĆdE","UTF8_BINARY", "abćde");
    assertLower("AbĆdE","UTF8_BINARY_LCASE", "abćde");
    assertLower("AbĆdE","UNICODE", "abćde");
    assertLower("AbĆdE","UNICODE_CI", "abćde");
    // Variable byte length characters
    assertLower("aB世De", "UTF8_BINARY", "ab世de");
    assertLower("ÄBĆΔE", "UTF8_BINARY", "äbćδe");
    assertLower("aB世De", "UTF8_BINARY_LCASE", "ab世de");
    assertLower("ÄBĆΔE", "UTF8_BINARY_LCASE", "äbćδe");
    assertLower("aB世De", "UNICODE", "ab世de");
    assertLower("ÄBĆΔE", "UNICODE", "äbćδe");
    assertLower("aB世De", "UNICODE_CI", "ab世de");
    assertLower("ÄBĆΔE", "UNICODE_CI", "äbćδe");
    // Case-variable character length
    assertLower("İo", "UTF8_BINARY","i̇o");
    assertLower("İo", "UTF8_BINARY_LCASE","i̇o");
    assertLower("İo", "UNICODE","i̇o");
    assertLower("İo", "UNICODE_CI","i̇o");
  }

  private void assertInitCap(String target, String collationName, String expected)
          throws SparkException {
    UTF8String target_utf8 = UTF8String.fromString(target);
    UTF8String expected_utf8 = UTF8String.fromString(expected);
    int collationId = CollationFactory.collationNameToId(collationName);
    assertEquals(expected_utf8, CollationSupport.InitCap.exec(target_utf8, collationId));
  }

  @Test
  public void testInitCap() throws SparkException {
    // Edge cases
    assertInitCap("", "UTF8_BINARY", "");
    assertInitCap("", "UTF8_BINARY_LCASE", "");
    assertInitCap("", "UNICODE", "");
    assertInitCap("", "UNICODE_CI", "");
    // Basic tests
    assertInitCap("ABCDE", "UTF8_BINARY", "Abcde");
    assertInitCap("ABCDE", "UTF8_BINARY_LCASE", "Abcde");
    assertInitCap("ABCDE", "UNICODE", "Abcde");
    assertInitCap("ABCDE", "UNICODE_CI", "Abcde");
    // Uppercase present
    assertInitCap("AbCdE", "UTF8_BINARY", "Abcde");
    assertInitCap("aBcDe", "UTF8_BINARY", "Abcde");
    assertInitCap("AbCdE", "UTF8_BINARY_LCASE", "Abcde");
    assertInitCap("aBcDe", "UTF8_BINARY_LCASE", "Abcde");
    assertInitCap("AbCdE", "UNICODE", "Abcde");
    assertInitCap("aBcDe", "UNICODE", "Abcde");
    assertInitCap("AbCdE", "UNICODE_CI", "Abcde");
    assertInitCap("aBcDe", "UNICODE_CI", "Abcde");
    // Accent letters
    assertInitCap("AbĆdE","UTF8_BINARY", "Abćde");
    assertInitCap("AbĆdE","UTF8_BINARY_LCASE", "Abćde");
    assertInitCap("AbĆdE","UNICODE", "Abćde");
    assertInitCap("AbĆdE","UNICODE_CI", "Abćde");
    // Variable byte length characters
    assertInitCap("aB世De", "UTF8_BINARY", "Ab世de");
    assertInitCap("ÄBĆΔE", "UTF8_BINARY", "Äbćδe");
    assertInitCap("aB世De", "UTF8_BINARY_LCASE", "Ab世de");
    assertInitCap("ÄBĆΔE", "UTF8_BINARY_LCASE", "Äbćδe");
    assertInitCap("aB世De", "UNICODE", "Ab世de");
    assertInitCap("ÄBĆΔE", "UNICODE", "Äbćδe");
    assertInitCap("aB世De", "UNICODE_CI", "Ab世De");
    assertInitCap("ÄBĆΔE", "UNICODE_CI", "Äbćδe");
    // Case-variable character length
    assertInitCap("İo", "UTF8_BINARY","İo");
    assertInitCap("İo", "UTF8_BINARY_LCASE","İo");
    assertInitCap("İo", "UNICODE","İo");
    assertInitCap("İo", "UNICODE_CI","İo");
  }

  // TODO: Test more collation-aware string expressions.

  /**
   * Collation-aware regexp expressions.
   */

  // TODO: Test more collation-aware regexp expressions.

  /**
   * Other collation-aware expressions.
   */

  // TODO: Test other collation-aware expressions.

}
