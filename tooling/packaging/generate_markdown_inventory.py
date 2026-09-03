from pathlib import Path
import sys
from release_metadata import included_files, repository_root

root=repository_root(__file__, sys.argv[1] if len(sys.argv)>1 else None)
out=root/'release/MARKDOWN-INVENTORY.txt'
out.parent.mkdir(parents=True, exist_ok=True)
paths=[path.relative_to(root).as_posix() for path in included_files(root) if path.suffix.lower()=='.md']
out.write_text('\n'.join(paths)+'\n')
print(f'wrote {out}: {len(paths)} files')
