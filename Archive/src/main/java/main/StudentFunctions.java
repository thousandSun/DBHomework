package main;

import hashdb.HashFile;
import hashdb.HashHeader;
import hashdb.Vehicle;
import misc.MutableInteger;
import misc.ReturnCodes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.util.Arrays;

public class StudentFunctions {
    /**
     * hashCreate
     * This funcAon creates a hash file containing only the HashHeader record.
     * • If the file already exists, return RC_FILE_EXISTS
     * • Create the binary file by opening it.
     * • Write the HashHeader record to the file at RBN 0.
     * • close the file.
     * • return RC_OK.
     */
    public static int hashCreate(String fileName, HashHeader hashHeader) {
        File file = new File(fileName);
        if(file.exists()){
            return ReturnCodes.RC_FILE_EXISTS;
        }
        try{
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            randomAccessFile.write(hashHeader.toByteArray(), 0, hashHeader.toByteArray().length);
            randomAccessFile.close();
        }catch (FileNotFoundException e){
            return ReturnCodes.RC_FILE_NOT_FOUND;
        } catch (IOException e) {
            return ReturnCodes.RC_LOC_NOT_WRITTEN;
        }
        return ReturnCodes.RC_OK;
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
        File file = new File(fileName);
        if(!file.exists()){
            return ReturnCodes.RC_FILE_NOT_FOUND;
        }
        try{
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            randomAccessFile.seek(0);
            HashHeader hashHeader = new HashHeader();
            byte[] header = new byte[HashHeader.MAX_REC_SIZE];
            randomAccessFile.read(header, 0, header.length);
            hashHeader.fromByteArray(header);
            hashFile.setHashHeader(hashHeader);
            hashFile.setFile(randomAccessFile);
        }catch(FileNotFoundException e){
            return ReturnCodes.RC_FILE_NOT_FOUND;
        } catch (IOException e) {
            return ReturnCodes.RC_HEADER_NOT_FOUND;
        }
        return ReturnCodes.RC_OK;
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
        int rbn = main.P2Main.hash(vehicle.getVehicleId(), hashFile.getHashHeader().getMaxHash());
        int i;
        for(i = 1; i <= hashFile.getHashHeader().getMaxProbe(); i++){
            Vehicle temp = new Vehicle();
            int ret = readRec(hashFile, rbn, temp);
            if(ret == ReturnCodes.RC_LOC_NOT_FOUND || temp.getVehicleId()[1] == '\0'){
                writeRec(hashFile, rbn, vehicle);
                return ReturnCodes.RC_OK;
            }else if(Arrays.equals(temp.getVehicleId(), vehicle.getVehicleId())){
                return ReturnCodes.RC_REC_EXISTS;
            }
            rbn = (rbn + 1) % hashFile.getHashHeader().getMaxHash();
        }
        return ReturnCodes.RC_TOO_MANY_COLLISIONS;
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
        int rba = rbn * hashFile.getHashHeader().getRecSize();
        try{
            hashFile.getFile().seek(rba);
            byte[] bytes = new byte[Vehicle.sizeOf()];
            hashFile.getFile().read(bytes, 0, Vehicle.sizeOf());
            if(bytes[1] != '\0'){
                vehicle.fromByteArray(bytes);
            }
        } catch (IOException e) {
            return ReturnCodes.RC_LOC_NOT_FOUND;
        }
        return ReturnCodes.RC_OK;
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
        int rba = rbn * hashFile.getHashHeader().getRecSize();
        try{
            hashFile.getFile().seek(rba);
            char[] chars = vehicle.toFileChars();
            for(int i = 0; i < chars.length; i++) {
                hashFile.getFile().writeChar(chars[i]);
            }
        } catch (IOException e) {
            return ReturnCodes.RC_LOC_NOT_WRITTEN;
        }
        return ReturnCodes.RC_OK;
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
    public static int vehicleRead(HashFile hashFile, MutableInteger rbn, Vehicle vehicle) {
        int i;
        int returnval = ReturnCodes.RC_REC_NOT_FOUND;
        Vehicle temp = new Vehicle();
        for(i = 1; i <= hashFile.getHashHeader().getMaxHash(); i++){
            int ret = readRec(hashFile, rbn.intValue(), temp);
            if(ret == ReturnCodes.RC_OK && Arrays.equals(temp.getVehicleId(), vehicle.getVehicleId())){
                vehicle.fromByteArray(temp.toByteArray());
                returnval = ReturnCodes.RC_OK;
            }
            rbn.set((rbn.intValue() + 1)%hashFile.getHashHeader().getMaxHash());
        }

        return returnval;
//        return ReturnCodes.RC_REC_NOT_FOUND;
    }
    public static int vehicleUpdate(HashFile hashFile, Vehicle vehicle){
        int i;
        int rbn = main.P2Main.hash(vehicle.getVehicleId(), hashFile.getHashHeader().getMaxHash());
        Vehicle tmp = new Vehicle();

        for(i = 1; i < hashFile.getHashHeader().getMaxHash(); i++){
            int ret = readRec(hashFile, rbn, tmp);
            if(Arrays.equals(tmp.getVehicleId(), vehicle.getVehicleId())){
                writeRec(hashFile, rbn, vehicle);
                return ReturnCodes.RC_OK;
            }
            rbn = (rbn + 1) % hashFile.getHashHeader().getMaxHash();
        }
        return ReturnCodes.RC_REC_NOT_FOUND;
    }

    public static int vehicleDelete(HashFile hashFile, char []vehicleId){
        return ReturnCodes.RC_NOT_IMPLEMENTED;
    }
}
