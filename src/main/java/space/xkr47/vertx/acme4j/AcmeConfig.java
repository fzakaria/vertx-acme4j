/*
 * Copyright 2017 Jonas Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package space.xkr47.vertx.acme4j;

import space.xkr47.vertx.acme4j.util.Struct;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AcmeConfig extends Struct {
    public LocalTime renewalCheckTime;
    public Map<String,Account> accounts;

    public void validate() {
        if (renewalCheckTime == null) throw new NullPointerException("renewalCheckTime");
        if (accounts == null) throw new NullPointerException("accounts");
        accounts.values().stream().forEach(Account::validate);
        List<String> duplicateHostNames = accounts.values()
                .stream()
                .filter(a -> a.enabled)
                .flatMap(a -> a.certificates.values()
                        .stream()
                        .filter(c -> c.enabled)
                        .flatMap(c -> c.hostnames.stream()))
                .collect(Collectors.groupingBy(s -> s))
                .values()
                .stream()
                .filter(l -> l.size() > 1)
                .map(l -> l.iterator().next())
                .collect(Collectors.toList());
        if (!duplicateHostNames.isEmpty()) {
            throw new IllegalArgumentException("Duplicate hostnames found among accounts and certificates: " + duplicateHostNames);
        }
        List<String> certsMarkedDefault = accounts.entrySet()
                .stream()
                .filter(a -> a.getValue().enabled)
                .flatMap(a -> a.getValue().certificates.entrySet()
                        .stream()
                        .filter(e -> e.getValue().enabled && e.getValue().defaultCert)
                        .map(e -> "account " + a.getKey() + " certificate " + e.getKey()))
                .collect(Collectors.toList());
        if (certsMarkedDefault.size() > 1) {
            throw new IllegalArgumentException("Multiple certificates marked default: " + certsMarkedDefault);
        }
    }

    public static class Account extends Struct {
        public boolean enabled = true;
        public String providerUrl;
        public String acceptedAgreementUrl;
        public List<String> contactURIs;
        public int minimumValidityDays;
        public Map<String, Certificate> certificates;

        public void validate() {
            if (!enabled) return;
            if (providerUrl == null || providerUrl.isEmpty()) throw new NullPointerException("providerUrl");
            if (minimumValidityDays < 1) throw new IllegalArgumentException("minimumValidityDays must be greater than zero");
            if (certificates == null) throw new NullPointerException("certificates");
            certificates.values().stream().forEach(Certificate::validate);
        }

        @Override
        public Account clone() {
            Account c = (Account) super.clone();
            c.certificates = cloneMapValues(certificates);
            return c;
        }
    }

    public static class Certificate extends Struct {
        public boolean enabled = true;
        public boolean defaultCert;
        public String organization;
        public List<String> hostnames;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Certificate that = (Certificate) o;

            if (enabled != that.enabled) return false;
            if (defaultCert != that.defaultCert) return false;
            if (organization != null ? !organization.equals(that.organization) : that.organization != null)
                return false;
            return hostnames != null ? eq(hostnames, that.hostnames) : that.hostnames == null;
        }
        @Override
        public int hashCode() {
            int result = (enabled ? 1 : 0);
            result = 31 * result + (defaultCert ? 1 : 0);
            result = 31 * result + (organization != null ? organization.hashCode() : 0);
            result = 31 * result + (hostnames != null ? hc(hostnames) : 0);
            return result;
        }


        /**
         * Hostnames lists considered equal if first elements are the same and the lists treated as a set are equal
         */
        private boolean eq(List<String> a, List<String> b) {
            if (a.isEmpty()) return b.isEmpty();
            if (b.isEmpty()) return false;
            if (!a.get(0).equals(b.get(0))) return false;
            return new HashSet<>(a).equals(new HashSet<>(b));
        }

        private int hc(List<String> a) {
            if (a.isEmpty()) return a.hashCode();
            return a.get(0).hashCode() ^ new HashSet<>(a).hashCode();
        }

        @Override
        public Certificate clone() {
            return (Certificate) super.clone();
        }

        public void validate() {
            if (!enabled) return;
            if (organization == null) throw new NullPointerException("organization");
            if (hostnames == null || hostnames.isEmpty()) throw new NullPointerException("hostnames");
        }
    }

    @Override
    public AcmeConfig clone() {
        AcmeConfig c = (AcmeConfig) super.clone();
        c.accounts = cloneMapValues(accounts);
        return c;
    }
}
