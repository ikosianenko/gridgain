﻿// @csharp.file.header

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

namespace GridGain.Client.Impl.Portable
{
    using System;
    using System.Collections.Generic;
    using System.IO;
    using GridGain.Client.Portable;

    using PU = GridGain.Client.Impl.Portable.GridClientPortableUilts;
    using PSH = GridGain.Client.Impl.Portable.GridClientPortableSystemHandlers;

    /**
     * <summary>Portable write context.</summary>
     */ 
    internal class GridClientPortableWriteContext
    {
        /** Type descriptors. */
        private readonly IDictionary<Type, GridClientPortableTypeDescriptor> descs;

        /** Writer. */
        private readonly GridClientPortableWriterImpl writer;

        /** Start position in the stream. */
        private readonly int startPos;

        /** Handles. */
        private IDictionary<GridClientPortableObjectHandle, int> hnds;

        /** Current type ID. */
        private int curTypeId;

        /** Current mapper. */
        private GridClientPortableIdResolver curMapper;
        
        /** Current raw flag. */
        private bool curRaw;

        /** Current raw position. */
        private long curRawPos;
                                
        /**
         * <summary>Constructor.</summary>
         * <param name="descs">Type descriptors.</param>
         * <param name="stream">Output stream.</param>
         */
        public GridClientPortableWriteContext(IDictionary<Type, GridClientPortableTypeDescriptor> descs,
            Stream stream)
        {
            this.descs = descs;

            Stream = stream;

            startPos = (int)stream.Position;

            writer = new GridClientPortableWriterImpl(this);
        }
        
        /**
         * <summary>Current type ID.</summary>
         */
        public int CurrentTypeId
        {
            get { return curTypeId; }
        }

        /**
         * <summary>Current ID mapper.</summary>
         */
        public GridClientPortableIdResolver CurrentMapper
        {
            get { return curMapper; }
        }

        /**
         * <summary>Current raw flag.</summary>
         */
        public bool CurrentRaw
        {
            get { return curRaw; }
            set { curRaw = value; }
        }

        /**
         * <summary>Current raw position.</summary>
         */
        public long CurrentRawPosition
        {
            get { return curRawPos; }
            set { curRawPos = value; }
        }

        /**
         * <summary>Stream.</summary>
         */
        public Stream Stream
        {
            get;
            private set;
        }

        /**
         * <summary>Write object to the context.</summary>
         * <param name="obj">Object.</param>
         */ 
        public void Write(object obj)
        {
            // 1. Write null.
            if (obj == null)
            {
                Stream.WriteByte(PU.HDR_NULL);

                return;
            }
                
            // 2. Try writting as well-known type.
            int pos = (int)Stream.Position;

            Type type = obj.GetType();

            GridClientPortableSystemWriteDelegate sysHandler = PSH.WriteHandler(type);

            if (sysHandler != null)
            {
                sysHandler.Invoke(this, pos, obj);

                return;
            }
            
            // 3. Dealing with handles.
            if (hnds == null)
                hnds = new Dictionary<GridClientPortableObjectHandle, int>();

            GridClientPortableObjectHandle hnd = new GridClientPortableObjectHandle(obj);

            int hndPos;

            if (hnds.TryGetValue(hnd, out hndPos))
            {
                Stream.WriteByte(PU.HDR_HND);

                // Handle is written as difference between current position and handle position.
                PU.WriteInt((int)Stream.Position - hndPos, Stream);

                return;
            }
            else
                // Cache absolute handle position.
                hnds.Add(hnd, (int)Stream.Position);

            // 4. Get descriptor.
            GridClientPortableTypeDescriptor desc;
                
            if (!descs.TryGetValue(type, out desc))
                throw new GridClientPortableException("Unsupported object type [type=" + type +
                    ", object=" + obj + ']');

            // 5. Write header.
            Stream.WriteByte(PU.HDR_FULL);
            PU.WriteBoolean(desc.UserType, Stream);
            PU.WriteInt(desc.TypeId, Stream);
            PU.WriteInt(obj.GetHashCode(), Stream);

            // 6. Skip length as it is not known in the first place.
            Stream.Seek(8, SeekOrigin.Current);

            // 7. Preserve old frame.
            int oldTypeId = curTypeId;
            GridClientPortableIdResolver oldMapper = curMapper;
            bool oldRaw = curRaw;
            long oldRawPos = curRawPos;

            // 8. Push new frame.
            curTypeId = desc.TypeId;
            curMapper = desc.Mapper;
            curRaw = false;
            curRawPos = 0;

            // 9. Write object fields.
            desc.Serializer.WritePortable(obj, writer);

            // 10. Calculate and write length.
            WriteLength(Stream, pos, Stream.Position, CurrentRawPosition);

            // 11. Restore old frame.
            curTypeId = oldTypeId;
            curMapper = oldMapper;
            curRaw = oldRaw;
            curRawPos = oldRawPos;
        }

        /**
         * <summary>Write lengths.</summary>
         * <param name="stream">Stream</param>
         * <param name="pos">Initial position.</param>
         * <param name="retPos">Return position</param>
         * <param name="rawPos">Raw data position.</param>
         */
        private static void WriteLength(Stream stream, long pos, long retPos, long rawPos)
        {
            stream.Seek(pos + 10, SeekOrigin.Begin);

            int len = (int)(retPos - pos);

            PU.WriteInt(len, stream);

            if (rawPos != 0)
                // When set, it is difference between object head and raw position.
                PU.WriteInt((int)(rawPos - pos), stream);
            else
                // When no set, it is equal to object length. 
                PU.WriteInt(len, stream);

            stream.Seek(retPos, SeekOrigin.Begin);
        }
    }
}