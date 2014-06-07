package org.squirrelsql.session.sql;

import java.util.Date;

public class SQLHistoryEntry
{
   private String _normalizedSql;
   private String _sql;
   private boolean _new = true;
   private Date _stamp = new Date();

   public SQLHistoryEntry()
   {
      // For deserialization
   }

   public SQLHistoryEntry(String sql)
   {
      setSql(sql);
   }

   @Override
   public String toString()
   {
      return _normalizedSql;
   }

   @Override
   public boolean equals(Object o)
   {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      SQLHistoryEntry that = (SQLHistoryEntry) o;

      if (_normalizedSql != null ? !_normalizedSql.equals(that._normalizedSql) : that._normalizedSql != null) return false;

      return true;
   }

   @Override
   public int hashCode()
   {
      return _normalizedSql != null ? _normalizedSql.hashCode() : 0;
   }


   public String getSql()
   {
      return _sql;
   }

   public void setSql(String sql)
   {
      _sql = sql.trim();

      String buf = _sql.replaceAll("\\n", " ");

      int bufLen = buf.length();
      buf = buf.replaceAll("  ", " ");

      while (buf.length() < bufLen)
      {
         bufLen = buf.length();
         buf = buf.replaceAll("  ", " ");
      }

      _normalizedSql = buf;
   }

   public boolean isNew()
   {
      return _new;
   }

   public void setNew(boolean aNew)
   {
      _new = aNew;
   }

   public Date getStamp()
   {
      return _stamp;
   }

   public void setStamp(Date stamp)
   {
      _stamp = stamp;
   }
}
