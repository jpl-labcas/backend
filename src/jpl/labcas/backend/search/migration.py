"""Solr migration tool for exporting and importing Solr cores between instances."""

from __future__ import annotations

import argparse
import json
import sys
from datetime import datetime
from pathlib import Path
from typing import List, Optional

import httpx


class SolrMigrator:
    """Handles migration of Solr cores between instances."""

    def __init__(self, url: str, backup_dir: Path, verify_ssl: bool = False):
        self.url = url.rstrip('/')
        self.backup_dir = backup_dir
        self.verify_ssl = verify_ssl
        self.backup_dir.mkdir(parents=True, exist_ok=True)

    def check_connectivity(self, url: str) -> bool:
        """Check if Solr instance is accessible."""
        try:
            # Use admin/cores endpoint which is available in older Solr versions
            response = httpx.get(
                f"{url}/admin/cores?action=STATUS&wt=json",
                verify=self.verify_ssl,
                timeout=10
            )
            return response.status_code == 200
        except Exception as e:
            print(f"Error connecting to {url}: {e}")
            return False

    def get_cores(self) -> List[str]:
        """Get list of cores from Solr instance."""
        try:
            response = httpx.get(
                f"{self.url}/admin/cores?action=STATUS&wt=json",
                verify=self.verify_ssl,
                timeout=30
            )
            response.raise_for_status()
            data = response.json()
            cores = list(data.get('status', {}).keys())
            return sorted(cores)
        except Exception as e:
            print(f"Error getting cores: {e}")
            return []

    def export_core(self, core: str) -> Optional[Path]:
        """Export all documents from a core."""
        print(f"\n{'='*60}")
        print(f"Exporting core: {core}")
        print(f"{'='*60}")

        output_file = self.backup_dir / f"{core}.json"
        all_docs = []
        cursor_mark = "*"
        total_exported = 0

        # First, get total count
        try:
            count_response = httpx.get(
                f"{self.url}/{core}/select",
                params={"q": "*:*", "rows": 0, "wt": "json"},
                verify=self.verify_ssl,
                timeout=30
            )
            count_response.raise_for_status()
            count_data = count_response.json()
            total_docs = count_data.get('response', {}).get('numFound', 0)
            print(f"Total documents in core: {total_docs}")
        except Exception as e:
            print(f"Warning: Could not get document count: {e}")
            total_docs = None

        # Export documents using cursorMark pagination
        while True:
            try:
                params = {
                    "q": "*:*",
                    "rows": 1000,  # Batch size
                    "wt": "json",
                    "sort": "id asc",
                    "cursorMark": cursor_mark
                }

                response = httpx.get(
                    f"{self.url}/{core}/select",
                    params=params,
                    verify=self.verify_ssl,
                    timeout=60
                )
                response.raise_for_status()
                data = response.json()

                docs = data.get('response', {}).get('docs', [])
                next_cursor = data.get('nextCursorMark')

                if not docs:
                    break

                all_docs.extend(docs)
                total_exported += len(docs)

                if total_docs:
                    percent = (total_exported / total_docs) * 100
                    print(f"  Progress: {total_exported}/{total_docs} ({percent:.1f}%)")
                else:
                    print(f"  Exported: {total_exported} documents")

                # Check if we're done
                if not next_cursor or next_cursor == cursor_mark:
                    break

                cursor_mark = next_cursor

            except Exception as e:
                print(f"Error during export: {e}")
                if all_docs:
                    print(f"  Saving {len(all_docs)} documents exported so far...")
                    break
                else:
                    return None

        # Save to file
        if all_docs:
            with open(output_file, 'w') as f:
                json.dump(all_docs, f, indent=2)
            print(f"\n✓ Successfully exported {len(all_docs)} documents to {output_file}")
            return output_file
        else:
            print(f"\n✗ No documents found in core {core}")
            return None

    def import_core(self, core: str, input_file: Path) -> bool:
        """Import documents into a core."""
        print(f"\n{'='*60}")
        print(f"Importing core: {core}")
        print(f"{'='*60}")

        # Check if core exists
        try:
            response = httpx.get(
                f"{self.url}/admin/cores?action=STATUS&core={core}&wt=json",
                verify=self.verify_ssl,
                timeout=30
            )
            if response.status_code != 200:
                print(f"\n⚠ Warning: Core '{core}' does not exist in Solr instance.")
                print(f"  You need to create it first. Visit:")
                print(f"  {self.url}/admin/cores?action=CREATE&name={core}&instanceDir={core}")
                response = input("\n  Press Enter after creating the core, or 's' to skip: ")
                if response.lower() == 's':
                    return False
        except Exception as e:
            print(f"Error checking core status: {e}")
            return False

        # Load documents
        try:
            with open(input_file, 'r') as f:
                docs = json.load(f)
        except Exception as e:
            print(f"Error reading export file: {e}")
            return False

        if not docs:
            print("No documents to import")
            return False

        print(f"Importing {len(docs)} documents...")

        # Send update request in batches to avoid memory issues
        batch_size = 1000
        total_imported = 0

        for i in range(0, len(docs), batch_size):
            batch = docs[i:i + batch_size]
            batch_payload = {"add": batch}

            try:
                response = httpx.post(
                    f"{self.url}/{core}/update",
                    json=batch_payload,
                    params={"commit": "true" if i + batch_size >= len(docs) else "false"},
                    verify=self.verify_ssl,
                    timeout=120
                )
                response.raise_for_status()

                total_imported += len(batch)
                percent = (total_imported / len(docs)) * 100
                print(f"  Progress: {total_imported}/{len(docs)} ({percent:.1f}%)")

            except Exception as e:
                print(f"\n✗ Error importing batch: {e}")
                if hasattr(e, 'response') and e.response is not None:
                    try:
                        error_data = e.response.json()
                        print(f"  Response: {json.dumps(error_data, indent=2)}")
                    except:
                        print(f"  Response: {e.response.text[:500]}")
                return False

        # Final commit
        try:
            response = httpx.post(
                f"{self.url}/{core}/update",
                json={},
                params={"commit": "true"},
                verify=self.verify_ssl,
                timeout=30
            )
            response.raise_for_status()
        except Exception as e:
            print(f"Warning: Error during final commit: {e}")

        print(f"\n✓ Successfully imported {total_imported} documents into {core}")
        return True

    def export_mode(self, cores: Optional[List[str]] = None):
        """Run export mode: export cores from Solr to files."""
        print("Solr Export Tool")
        print("=" * 60)
        print(f"Solr URL: {self.url}")
        print(f"Backup directory: {self.backup_dir}")
        print()

        # Check connectivity
        print("Checking connectivity...")
        if not self.check_connectivity(self.url):
            print(f"✗ Cannot connect to Solr at {self.url}")
            sys.exit(1)
        print(f"✓ Solr is accessible")
        print()

        # Get cores to export
        if cores is None:
            cores = self.get_cores()
            if not cores:
                print("✗ No cores found in Solr instance")
                sys.exit(1)
            print(f"Found cores: {', '.join(cores)}")
        else:
            print(f"Exporting specified cores: {', '.join(cores)}")

        # Export phase
        print("\n" + "=" * 60)
        print("EXPORT PHASE")
        print("=" * 60)
        exported_files = {}

        for core in cores:
            exported_file = self.export_core(core)
            if exported_file:
                exported_files[core] = exported_file

        if not exported_files:
            print("\n✗ No cores were successfully exported")
            sys.exit(1)

        print("\n" + "=" * 60)
        print("EXPORT SUMMARY")
        print("=" * 60)
        print(f"Successfully exported: {len(exported_files)}/{len(cores)} cores")
        print(f"Files saved in: {self.backup_dir}")
        print("\nNext steps:")
        print(f"1. Use import mode to load these files into another Solr instance:")
        print(f"   migrate-solr --mode import <solr_url> --backup-dir {self.backup_dir}")

    def import_mode(self, cores: Optional[List[str]] = None):
        """Run import mode: import cores from files into Solr."""
        print("Solr Import Tool")
        print("=" * 60)
        print(f"Solr URL: {self.url}")
        print(f"Backup directory: {self.backup_dir}")
        print()

        # Check connectivity
        print("Checking connectivity...")
        if not self.check_connectivity(self.url):
            print(f"✗ Cannot connect to Solr at {self.url}")
            sys.exit(1)
        print(f"✓ Solr is accessible")
        print()

        # Find export files
        json_files = list(self.backup_dir.glob("*.json"))
        if not json_files:
            print(f"✗ No JSON export files found in {self.backup_dir}")
            print("  Run export mode first to create export files.")
            sys.exit(1)

        # Extract core names from filenames
        available_cores = [f.stem for f in json_files]
        print(f"Found export files for cores: {', '.join(available_cores)}")

        # Filter cores if specified
        if cores is not None:
            # Check that all specified cores have export files
            missing = [c for c in cores if f"{c}.json" not in [f.name for f in json_files]]
            if missing:
                print(f"✗ Export files not found for cores: {', '.join(missing)}")
                sys.exit(1)
            cores_to_import = cores
        else:
            cores_to_import = available_cores

        print(f"Importing cores: {', '.join(cores_to_import)}")

        # Import phase
        print("\n" + "=" * 60)
        print("IMPORT PHASE")
        print("=" * 60)

        success_count = 0
        for core in cores_to_import:
            export_file = self.backup_dir / f"{core}.json"
            if export_file.exists():
                if self.import_core(core, export_file):
                    success_count += 1
            else:
                print(f"\n✗ Export file not found: {export_file}")

        print("\n" + "=" * 60)
        print("IMPORT SUMMARY")
        print("=" * 60)
        print(f"Successfully imported: {success_count}/{len(cores_to_import)} cores")
        print("\nNext steps:")
        print("1. Verify data in Solr instance")
        print("2. Update your application configuration")
        print("3. Test your application thoroughly")


def main():
    """Main entry point for the Solr migration tool."""
    parser = argparse.ArgumentParser(
        description="Export or import Solr cores",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Export all cores from a Solr instance
  migrate-solr --mode export https://localhost:8984/solr

  # Export specific cores
  migrate-solr --mode export https://localhost:8984/solr --cores collections,datasets

  # Import cores into a Solr instance
  migrate-solr --mode import https://newhost:8983/solr --backup-dir /tmp/solr-backup-20240101-120000

  # Import specific cores
  migrate-solr --mode import https://newhost:8983/solr --backup-dir /tmp/solr-backup-20240101-120000 --cores collections,datasets
        """
    )
    parser.add_argument('url', help='URL of the Solr instance')
    parser.add_argument('-m', '--mode', required=True, choices=['export', 'import'],
                        help='Operation mode: export (save cores to files) or import (load cores from files)')
    parser.add_argument('--backup-dir', default=None,
                        help='Directory for backup files (default: /tmp/solr-backup-<timestamp> for export, current dir for import)')
    parser.add_argument('--cores', default=None,
                        help='Comma-separated list of cores to process (default: all cores)')
    parser.add_argument('--verify-ssl', action='store_true',
                        help='Verify SSL certificates (default: False)')

    args = parser.parse_args()

    # Set backup directory
    if args.backup_dir:
        backup_dir = Path(args.backup_dir)
    else:
        if args.mode == 'export':
            timestamp = datetime.now().strftime('%Y%m%d-%H%M%S')
            backup_dir = Path(f"/tmp/solr-backup-{timestamp}")
        else:
            # For import, default to current directory
            backup_dir = Path.cwd()

    # Parse cores
    cores = None
    if args.cores:
        cores = [c.strip() for c in args.cores.split(',')]

    # Create migrator and run appropriate mode
    migrator = SolrMigrator(
        url=args.url,
        backup_dir=backup_dir,
        verify_ssl=args.verify_ssl
    )

    if args.mode == 'export':
        migrator.export_mode(cores=cores)
    else:
        migrator.import_mode(cores=cores)


if __name__ == '__main__':
    main()
