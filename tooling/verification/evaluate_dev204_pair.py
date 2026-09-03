#!/usr/bin/env python3
import argparse, os, subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--red',required=True);p.add_argument('--green',required=True);a=p.parse_args()
root=Path(__file__).resolve().parents[2]
env=os.environ.copy();env.setdefault('MAVEN_OPTS','-Xmx2g')
subprocess.run([str(root/'mvnw'),'-q','-DskipTests','package'],cwd=root,check=True,env=env)
subprocess.run(['java','-jar',str(root/'target/fdi-0.4.8.3.jar'),'dev204-evaluate','--red',a.red,'--green',a.green],check=True)
