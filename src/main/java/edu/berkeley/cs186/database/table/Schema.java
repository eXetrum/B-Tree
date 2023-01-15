package edu.berkeley.cs186.database.table;

import edu.berkeley.cs186.database.datatypes.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The Schema of a particular table.
 *
 * Properties:
 * `fields`: an ordered list of column names
 * `fieldTypes`: an ordered list of data types corresponding to the columns
 * `size`: physical size (in bytes) of a record conforming to this schema
 */
public class Schema {
  private List<String> fields;
  private List<DataType> fieldTypes;
  private int size;

  public Schema(List<String> fields, List<DataType> fieldTypes) {
    assert(fields.size() == fieldTypes.size());

    this.fields = fields;
    this.fieldTypes = fieldTypes;
    this.size = 0;

    for (DataType dt : fieldTypes) {
      this.size += dt.getSize();
    }
  }

  /**
   * Verifies that a list of DataTypes corresponds to this schema. A list of
   * DataTypes corresponds to this schema if the number of DataTypes in the
   * list equals the number of columns in this schema, and if each DataType has
   * the same type and size as the columns in this schema.
   *
   * @param values the list of values to check
   * @return a new Record with the DataTypes specified
   * @throws SchemaException if the values specified don't conform to this Schema
   */
  public Record verify(List<DataType> values) throws SchemaException {
    if(fieldTypes.size() != values.size()) throw new SchemaException(toString());
    for(int i = 0; i < fieldTypes.size(); ++i){
      if( fieldTypes.get(i).type() != values.get(i).type()
              || fieldTypes.get(i).getSize() != values.get(i).getSize() ) throw new SchemaException(toString());
    }

    return new Record(values);
  }

  /**
   * Serializes the provided record into a byte[]. Uses the DataTypes's
   * serialization methods. A serialized record is represented as the
   * concatenation of each serialized DataType. This method assumes that the
   * input record corresponds to this schema.
   *
   * @param record the record to encode
   * @return the encoded record as a byte[]
   */
  public byte[] encode(Record record) {
    List<DataType> values = record.getValues();
    byte [] buffer = new byte[size];
    int i = 0;

    for (DataType datatype : values ) {
      byte [] cur_bytes = datatype.getBytes();
      for(int j = 0; j < datatype.getSize(); ++j) buffer[i + j]  = cur_bytes[j];
      i += datatype.getSize();
    }
    return buffer;
  }

  /**
   * Takes a byte[] and decodes it into a Record. This method assumes that the
   * input byte[] represents a record that corresponds to this schema.
   *
   * @param input the byte array to decode
   * @return the decoded Record
   */
  public Record decode(byte[] input) {
    List<DataType> values = new ArrayList<DataType>();
    ByteBuffer wrapped = null;
    byte [] chunk = null;
    int offset = 0;
    for (DataType fieldtype : fieldTypes) {
      switch (fieldtype.type()) {
        case BOOL:
          chunk = (byte[]) Arrays.copyOfRange(input, offset, offset + fieldtype.getSize() );
          values.add(new BoolDataType( chunk[0] != 0 ));
          break;
        case FLOAT:
          chunk = (byte[]) Arrays.copyOfRange(input, offset, offset + fieldtype.getSize() );
          wrapped = ByteBuffer.wrap(chunk);
          values.add(new FloatDataType( wrapped.getFloat() ));
          break;
        case INT:
          chunk = (byte[]) Arrays.copyOfRange(input, offset, offset + fieldtype.getSize() );
          wrapped = ByteBuffer.wrap(chunk);
          values.add(new IntDataType( wrapped.getInt() ));
          break;
        case STRING:
          chunk = (byte[]) Arrays.copyOfRange(input, offset, offset + fieldtype.getSize() );
          String s = new String(chunk);
          values.add(new StringDataType( s, s.length() ) );
          break;
      }

      offset += fieldtype.getSize();
    }

    return new Record(values);
  }

  public int getEntrySize() {
    return this.size;
  }

  public List<String> getFieldNames() {
    return this.fields;
  }

  public List<DataType> getFieldTypes() {
    return this.fieldTypes;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Schema)) {
      return false;
    }

    Schema otherSchema = (Schema) other;

    if (this.fields.size() != otherSchema.fields.size()) {
      return false;
    }

    for (int i = 0; i < this.fields.size(); i++) {
      DataType thisType = this.fieldTypes.get(i);
      DataType otherType = this.fieldTypes.get(i);

      if (thisType.type() != otherType.type()) {
        return false;
      }

      if (thisType.equals(DataType.Types.STRING) && thisType.getSize() != otherType.getSize()) {
        return false;
      }
    }

    return true;
  }
}
