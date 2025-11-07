import json
import sys

def read_dict(inFile):
	with open(inFile, 'r') as f:
		return json.load(f)

def write_to_file(data, outFile):
	with open(outFile, "w") as file:
		json.dump(data, file, indent=4)

def main():
	if (len(sys.argv) != 3):
		exit(1)

	data = read_dict(sys.argv[1])
	for key in data:
		data[key] = "I want to know about {info.value}." + chr(97 + data[key])
	write_to_file(data, sys.argv[2])

if __name__ == "__main__":
	main()