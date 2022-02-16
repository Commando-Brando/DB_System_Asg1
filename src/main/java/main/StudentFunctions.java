package main;

import hashdb.HashFile;
import hashdb.HashHeader;
import hashdb.Vehicle;
import misc.ReturnCodes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import static misc.ReturnCodes.*;

// RandomAccessFile Docs https://docs.oracle.com/javase/8/docs/api/java/io/RandomAccessFile.html

public class StudentFunctions {
    /**
     * hashCreate
     * This function creates a hash file containing only the HashHeader record.
     * • If the file already exists, return RC_FILE_EXISTS
     * • Create the binary file by opening it.
     * • Write the HashHeader record to the file at RBN 0.
     * • close the file.
     * • return RC_OK.
     */
    public static int hashCreate(String fileName, HashHeader hashHeader) {

        // check if file exists
        File file = new File(fileName);
        if(file.exists())
            return ReturnCodes.RC_FILE_EXISTS;

        try {
            RandomAccessFile binaryFile;
            binaryFile = new RandomAccessFile(fileName, "rw");

            HashFile hFile = new HashFile();
            hFile.setHashHeader(hashHeader);
            hFile.setFile(binaryFile);
            byte[] bytes = hashHeader.toByteArray();
            binaryFile.write(bytes);
            binaryFile.close();

        } catch (IOException e) {
            System.out.println("IOException in hashCreate method");
            e.printStackTrace();
        }
        return RC_OK;
    }

    /**
     * hashOpen
     * This function opens an existing hash file which must contain a HashHeader record
     * , and sets the file member of hashFile
     * It returns the HashHeader record by setting the HashHeader member in hashFile
     * If it doesn't exist, return RC_FILE_NOT_FOUND.
     * Read the HashHeader record from file and return it through the parameter.
     * If the read fails, return RC_HEADER_NOT_FOUND.
     * return RC_OK
     */
    public static int hashOpen(String fileName, HashFile hashFile) {

        // check if file exists
        File file = new File(fileName);
        if(!file.exists())
            return ReturnCodes.RC_FILE_NOT_FOUND;

        try {
            RandomAccessFile binaryFile = new RandomAccessFile(fileName, "rw"); // opens RandomAccessFile for read/write
            int recordSize = hashFile.getHashHeader().getRecSize();
            byte[] headerBytes = new byte[recordSize];
            int numberOfBytesRead = binaryFile.read(headerBytes);
            if(numberOfBytesRead == -1) {
                return RC_HEADER_NOT_FOUND;
            }
            hashFile.setFile(binaryFile); // sets the HashFile's RandomAccessFile object pointer to the current file
            hashFile.getHashHeader().fromByteArray(headerBytes); // sets the HashFile's header to the one read in from the file
        } catch (IOException e) {
            System.out.println("IOException in hashOpen method");
            e.printStackTrace();
        }
        return RC_OK;
    }

    /**
     * vehicleInsert
     * This function inserts a vehicle into the specified file.
     * Determine the RBN using the Main class' hash function.
     * Use readRec to read the record at that RBN.
     * If that location doesn't exist
     * OR the record at that location has a blank vehicleId (i.e., empty string):
     * THEN Write this new vehicle record at that location using writeRec.
     * If that record exists and that vehicle's szVehicleId matches, return RC_REC_EXISTS.
     * (Do not update it.)
     * Otherwise, return RC_SYNONYM. a SYNONYM is the same thing as a HASH COLLISION
     * Note that in program #2, we will actually insert synonyms.
     */
    public static int vehicleInsert(HashFile hashFile, Vehicle vehicle) {
        Vehicle readRecVehicle = new Vehicle(); // makes new vehicle to be sent into readRec() and will be populated by the data at the rba
        int rbn = Main.hash(vehicle.getVehicleId(), hashFile.getHashHeader().getMaxHash());  // gets rbn based on vehicle hash value

        try {
            int readStatus = readRec(hashFile, rbn, readRecVehicle); // reads in the data at the rbn and saves the data to readRecVehicle
            if(readStatus == ReturnCodes.RC_LOC_NOT_FOUND || readRecVehicle.getVehicleId()[0] == 0){
                int writeRC = writeRec(hashFile, rbn, vehicle);
            } else if(readRecVehicle.getVehicleIdAsString().equals(vehicle.getVehicleIdAsString())) {
                return RC_REC_EXISTS;
            } else {
                return RC_SYNONYM;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return RC_OK;
    }

    /**
     * readRec(
     * This function reads a record at the specified RBN in the specified file.
     * Determine the RBA based on RBN and the HashHeader's recSize
     * Use seek to position the file in that location.
     * Read that record and return it through the vehicle parameter.
     * If the location is not found, return RC_LOC_NOT_FOUND.  Otherwise, return RC_OK.
     * Note: if the location is found, that does NOT imply that a vehicle
     * was written to that location.  Why?
      */
    public static int readRec(HashFile hashFile, int rbn, Vehicle vehicle) {
        int recordSize = hashFile.getHashHeader().getRecSize();
        long rba = (long) rbn * recordSize;
        try {
            // TODO: 2/12/2022 do you need to account for hashheader record when seeking since seek starts from beginning of the file
            hashFile.getFile().seek(rba);
            byte[] vehicleBytes = new byte[recordSize];
            int bytesRead = hashFile.getFile().read(vehicleBytes, 0, recordSize);
            if(bytesRead == -1) {
                return ReturnCodes.RC_LOC_NOT_FOUND;
            }
            vehicle.fromByteArray(vehicleBytes);
        } catch (IOException e) {
            System.out.println("IOException in readRec method");
            e.printStackTrace();
        }
        // TODO: 2/12/2022 look at last line in pdf instructions for this method. do we need to verify it is a proper vehicle record?
        return RC_OK;
    }

    /**
     * writeRec
     * This function writes a record to the specified RBN in the specified file.
     * Determine the RBA based on RBN and the HashHeader's recSize
     * Use seek to position the file in that location.
     * Write that record to the file.
     * If the write fails, return RC_LOC_NOT_WRITTEN.
     * Otherwise, return RC_OK.
     */
    public static int writeRec(HashFile hashFile, int rbn, Vehicle vehicle) {
        int recordSize = hashFile.getHashHeader().getRecSize();
        long rba = (long) rbn * recordSize; // calculates rba: rbn * record size
        try {
            hashFile.getFile().seek(rba);
            byte[] bytesToWrite; // initialize byte array based on record size
            bytesToWrite = vehicle.toByteArray(); // fill byte array with the appropriate vehicle bytes
            hashFile.getFile().write(bytesToWrite, 0, recordSize); // write the byte array to the file
        } catch (IOException e) {
            System.out.println("IOException in writeRec method");
            e.printStackTrace();
            return RC_LOC_NOT_WRITTEN;
        }
        return RC_OK;
    }

    /**
     * vehicleRead
     * This function reads the specified vehicle by its vehicleId.
     * Since the vehicleId was provided,
     * determine the RBN using the Main class' hash function.
     * Use readRec to read the record at that RBN.
     * If the vehicle at that location matches the specified vehicleId,
     * return the vehicle via the parameter and return RC_OK.
     * Otherwise, return RC_REC_NOT_FOUND
     */
    public static int vehicleRead(HashFile hashFile, int rbn, Vehicle vehicle) {
        Vehicle readRecVehicle = new Vehicle();
        int bytesRead = readRec(hashFile, rbn, readRecVehicle);
        if(readRecVehicle.getVehicleIdAsString().equals(vehicle.getVehicleIdAsString())) {
            vehicle.fromByteArray(readRecVehicle.toByteArray());
            return RC_OK;
        }
        return ReturnCodes.RC_REC_NOT_FOUND;
    }
}