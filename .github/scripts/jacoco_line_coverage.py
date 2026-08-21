#!/usr/bin/env python3
"""Print the overall LINE coverage percentage from a JaCoco XML report."""
import sys
import xml.etree.ElementTree as ET


def main():
    if len(sys.argv) != 2:
        print("usage: jacoco_line_coverage.py <jacoco-report.xml>", file=sys.stderr)
        sys.exit(1)

    report = ET.parse(sys.argv[1]).getroot()

    # Report-level counters are the <counter> elements that are direct children of <report>,
    # one per type, listed after every <package>. findall("counter") only matches those,
    # not the per-package/per-class ones nested inside <package>/<class>/etc.
    for counter in report.findall("counter"):
        if counter.get("type") == "LINE":
            covered = int(counter.get("covered"))
            missed = int(counter.get("missed"))
            total = covered + missed
            percent = (covered / total * 100) if total else 100.0
            print(f"{percent:.2f}")
            return

    print("no report-level LINE counter found", file=sys.stderr)
    sys.exit(1)


if __name__ == "__main__":
    main()
