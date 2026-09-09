#!/usr/bin/env python3
"""Upload built jars from release-assets/ to an existing Modrinth project."""

from __future__ import annotations

import json
import os
import re
import sys
import urllib.error
import urllib.request
import uuid
from pathlib import Path

API = "https://api.modrinth.com/v2"
PROJECT_ID = os.environ.get("MODRINTH_PROJECT_ID", "cST5iATf")
TOKEN = os.environ.get("MODRINTH_TOKEN", "")
REF_NAME = os.environ.get("GITHUB_REF_NAME", "")
UA = "BowAlert-CI/1.0 (github.com/half-drop/BowAlert)"
FABRIC_API_ID = "P7dR8mSH"

# jar filename stem patterns produced by build.gradle:
# bow-alert-1.21.11-1.0.0+mc1.21.11.jar
# bow-alert-26.2-1.0.0+mc26.2.jar
GAME_VERSIONS = ("1.21.11", "26.2")


def fail(message: str) -> None:
    print(f"::error::{message}", file=sys.stderr)
    raise SystemExit(1)


def request(method: str, path: str, *, json_body=None, files=None):
    boundary = "----BowAlertCI" + uuid.uuid4().hex
    body = bytearray()
    headers = {
        "Authorization": f"Bearer {TOKEN}",
        "User-Agent": UA,
    }

    if files is None:
        if json_body is not None:
            body = json.dumps(json_body).encode()
            headers["Content-Type"] = "application/json"
        req = urllib.request.Request(f"{API}{path}", data=body or None, method=method)
    else:
        def field(name: str, value) -> None:
            body.extend(f"--{boundary}\r\n".encode())
            body.extend(f'Content-Disposition: form-data; name="{name}"\r\n\r\n'.encode())
            body.extend(
                json.dumps(value, ensure_ascii=False).encode()
                if isinstance(value, (dict, list))
                else str(value).encode()
            )
            body.extend(b"\r\n")

        def file_part(name: str, filename: str, data: bytes) -> None:
            body.extend(f"--{boundary}\r\n".encode())
            body.extend(
                f'Content-Disposition: form-data; name="{name}"; filename="{filename}"\r\n'.encode()
            )
            body.extend("Content-Type: application/java-archive\r\n\r\n".encode())
            body.extend(data)
            body.extend(b"\r\n")

        for key, value in (json_body or {}).items():
            field(key, value)
        for name, (filename, data) in files.items():
            file_part(name, filename, data)
        body.extend(f"--{boundary}--\r\n".encode())
        headers["Content-Type"] = f"multipart/form-data; boundary={boundary}"
        req = urllib.request.Request(f"{API}{path}", data=bytes(body), method=method)

    for key, value in headers.items():
        req.add_header(key, value)

    try:
        with urllib.request.urlopen(req) as resp:
            raw = resp.read()
            if not raw:
                return resp.status, None
            try:
                return resp.status, json.loads(raw.decode())
            except json.JSONDecodeError:
                return resp.status, raw.decode(errors="replace")
    except urllib.error.HTTPError as exc:
        raw = exc.read()
        try:
            return exc.code, json.loads(raw.decode() or "null")
        except json.JSONDecodeError:
            return exc.code, raw.decode(errors="replace")


def main() -> None:
    if not TOKEN:
        fail("MODRINTH_TOKEN secret is not set")

    version = REF_NAME.lstrip("v") if REF_NAME else "0.0.0"
    if not re.fullmatch(r"\d+\.\d+\.\d+", version):
        fail(f"unexpected tag name: {REF_NAME!r}")

    assets = Path("release-assets")
    if not assets.is_dir():
        fail("release-assets directory is missing")

    jars: list[Path] = [
        path
        for path in sorted(assets.glob("*.jar"))
        if not path.name.endswith("-sources.jar")
        and not path.name.endswith("-javadoc.jar")
        and "dev" not in path.name
    ]
    if not jars:
        fail("no release jars found in release-assets/")

    status, existing = request("GET", f"/project/{PROJECT_ID}/version")
    if status >= 300:
        fail(f"failed to list versions: {status} {existing}")
    existing_numbers = {item.get("version_number") for item in existing or []}

    published = 0
    for jar in jars:
        matched = None
        for game_version in GAME_VERSIONS:
            if f"-{game_version}-" in jar.name or f"+mc{game_version}" in jar.name:
                matched = game_version
                break
        if matched is None:
            print(f"skip unrecognized jar: {jar.name}")
            continue

        version_number = f"{version}+mc{matched}"
        if version_number in existing_numbers:
            print(f"skip existing version: {version_number}")
            continue

        payload = {
            "name": f"v{version} (Minecraft {matched})",
            "version_number": version_number,
            "changelog": (
                f"Release v{version} for Minecraft {matched}.\n\n"
                f"GitHub Release: https://github.com/half-drop/BowAlert/releases/tag/{REF_NAME or 'v' + version}"
            ),
            "dependencies": [
                {"project_id": FABRIC_API_ID, "dependency_type": "required"}
            ],
            "game_versions": [matched],
            "version_type": "release",
            "loaders": ["fabric"],
            "featured": True,
            "project_id": PROJECT_ID,
            "file_parts": [jar.name],
            "primary_file": jar.name,
        }

        status, data = request(
            "POST",
            "/version",
            json_body={"data": payload},
            files={"file_parts": (jar.name, jar.read_bytes())},
        )
        if status >= 300:
            fail(f"failed to create version {version_number}: {status} {data}")
        print(f"published {version_number} id={data.get('id')}")
        published += 1

    if published == 0:
        print("nothing new to publish")
    else:
        print(f"published {published} version(s) to https://modrinth.com/mod/bowalert")


if __name__ == "__main__":
    main()
