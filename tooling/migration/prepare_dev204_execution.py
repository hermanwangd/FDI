#!/usr/bin/env python3
import argparse, os, subprocess
from pathlib import Path


def main(argv=None):
    parser=argparse.ArgumentParser()
    parser.add_argument('--scenario-pack',required=True)
    parser.add_argument('--output-dir',required=True)
    args=parser.parse_args(argv)
    root=Path(__file__).resolve().parents[2]
    env=os.environ.copy()
    env['MAVEN_OPTS']='-Xmx2g'
    subprocess.run([str(root/'mvnw'),'-q','-DskipTests','package'],cwd=root,check=True,env=env)
    subprocess.run(['java','-jar',str(root/'target/fdi-0.4.8.3.jar'),'dev204-prepare','--scenario-pack',args.scenario_pack,'--output-dir',args.output_dir],check=True)


if __name__ == '__main__':
    main()
