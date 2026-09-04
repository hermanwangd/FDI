#!/usr/bin/env python3
import argparse, os, subprocess
from pathlib import Path


def main(argv=None):
    parser=argparse.ArgumentParser()
    parser.add_argument('--red',required=True)
    parser.add_argument('--green',required=True)
    args=parser.parse_args(argv)
    root=Path(__file__).resolve().parents[2]
    env=os.environ.copy()
    env['MAVEN_OPTS']='-Xmx2g'
    subprocess.run([str(root/'mvnw'),'-q','-DskipTests','package'],cwd=root,check=True,env=env)
    subprocess.run(['java','-jar',str(root/'target/fdi-0.4.8.3.jar'),'dev204-evaluate','--red',args.red,'--green',args.green],check=True)


if __name__ == '__main__':
    main()
