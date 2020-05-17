airframe-codec
====

airframe-codec is an [MessagePack](https://msgpack.org)-based schema-on-read data transcoder for Scala and Scala.js.

With airframe-codec you can:
- Encode Scala objects (e.g., case classes, collection, etc.) into MessagePack format, and decode it. Object serialization/deserialization.
- Convert JDBC result sets into MessagePack
- Add you custom codec (implementing pack/unpack)
- You can use airframe-tablet is for reading CSV/TSV/JSON/JDBC data etc.    

airframe-codec supports schema-on-read data conversion.
For example, even if your data is string representation of integer values, e.g., "1", "2, "3", ..., 
airframe-codec can convert it into integers if the target schema (e.g., objects) requires integer values. 


- [Documentation](https://wvlet.org/airframe/docs/airframe-codec)

