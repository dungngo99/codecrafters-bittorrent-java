import com.google.gson.Gson;
import domain.ValueWrapper;
//import com.dampcake.bencode.Bencode;

public class Main {
  private static final Gson gson = new Gson();

  public static void main(String[] args) {
    if (args.length == 0) {
      return;
    }
    String command = args[0];
    if("decode".equals(command)) {
        String encodedValue = args[1];
        //String decoded;
      BEncoder bEncoder = new BEncoder(encodedValue);
      Object o;
        try {
          //decoded = decodeBencode(encodedValue);
          ValueWrapper vw = bEncoder.decode();
          o = ValueWrapper.convert(vw);
        } catch(RuntimeException e) {
          System.out.println(e.getMessage());
          return;
        }
        //System.out.println(gson.toJson(decoded));
        System.out.println(gson.toJson(o));
    } else {
      System.out.println("Unknown command: " + command);
    }
  }

  static String decodeBencode(String bEncodedString) {
    if (Character.isDigit(bEncodedString.charAt(0))) {
      int firstColonIndex = 0;
      for(int i = 0; i < bEncodedString.length(); i++) {
        if(bEncodedString.charAt(i) == ':') {
          firstColonIndex = i;
          break;
        }
      }
      int length = Integer.parseInt(bEncodedString.substring(0, firstColonIndex));
      return bEncodedString.substring(firstColonIndex+1, firstColonIndex+1+length);
    } else {
      throw new RuntimeException("Only strings are supported at the moment");
    }
  }
}
