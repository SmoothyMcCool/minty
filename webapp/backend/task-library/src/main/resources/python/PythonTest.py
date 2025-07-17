import json
import sys

def read_dict(inFile):
	with open(inFile, 'r') as f:
		return json.load(f)
	except FileNotFoundError:
		exit(-1)
	except json.JSONDecodeError:
		exit(-1)

def write_to_file(data, outFile):
	with open(filename, "w") as file:
		json.dump(data, file, indent=4)

def main():
	if (len(sys.argv) == 3):
		data = read_dict(sys.argv[1])
		write_to_file(data, sys.argv[2])

if __name__ == "__main__":
	main()