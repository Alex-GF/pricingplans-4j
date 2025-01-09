package io.github.isagroup.services.updaters;

import io.github.isagroup.exceptions.VersionException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum Version {
    V1_0(1, 0), V1_1(1, 1), V2_0(2, 0), V2_1(2, 1);

    private final int major;
    private final int minor;

    public static final Version LATEST = V2_1;

    Version(int major, int minor) {
        if (!isValid(major, minor)) {
            throw new IllegalStateException(String.format("Version of yaml %d.%d is unsupported", major, minor));
        }

        this.major = major;
        this.minor = minor;

    }

    public static Version version(Object version) {

        if (version == null) {
            throw new IllegalArgumentException("Cannot parse a null version");
        } else if (version instanceof Double) {
            return Version.version((Double) version);
        } else if (version instanceof String) {
            return Version.version((String) version);
        } else {
            throw new VersionException("Cannot parse " + version + " to a version");
        }

    }

    private static Version version(Double version) {
        String tempVer = String.valueOf(version);
        return Version.version(tempVer);
    }

    private static Version version(String version) {

        if (version.isEmpty() || version.isBlank()) {
            throw new VersionException("Version is blank");
        }

        String regex = "(\\d+)\\.(\\d+)";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(version);

        if (!matcher.matches()) {
            throw new VersionException("Invalid version \"" + version + "\", use <major>.<minor> version format");
        }

        int major;
        int minor;

        try {
            major = Integer.parseInt(matcher.group(1));

        } catch (NumberFormatException e) {
            throw new VersionException("major " + matcher.group(1) + " overflows an int");
        }

        try {
            minor = Integer.parseInt(matcher.group(2));

        } catch (NumberFormatException e) {
            throw new VersionException("minor " + matcher.group(2) + " overflows an int");
        }

        Version versionObj = Version.version(major, minor);

        if (versionObj == null) {
            throw new VersionException("Unsupported version " + major + "." + minor);
        }

        return versionObj;


    }

    public static Version version(int major, int minor) {

        if (major == 1) {
            if (minor == 0) {
                return V1_0;
            } else if (minor == 1) {
                return V1_1;
            }
        } else if (major == 2) {
            if (minor == 0) {
                return V2_0;
            } else if (minor == 1) {
                return V2_1;
            }
        }

        return null;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public static boolean isValid(int major, int minor) {
        boolean oneDotVersions = major == 1 && (minor == 0 || minor == 1);
        boolean twoDotVersions = major == 2 && (minor == 0 || minor == 1);
        return oneDotVersions || twoDotVersions;
    }

    public int compare(Version version) {

        if (this.major > version.getMajor()) {
            return 1;
        }

        if (this.major < version.getMajor()) {
            return -1;
        }

        if (this.minor > version.getMinor()) {
            return 1;
        } else if (this.minor < version.getMinor()) {
            return -1;
        }

        return 0;
    }

    @Override
    public String toString() {
        return this.major + "." + this.minor;
    }
}
