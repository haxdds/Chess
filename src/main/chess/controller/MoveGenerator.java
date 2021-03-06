package main.chess.controller;

import main.chess.structure.*;
import java.util.*;

/**
 *  Created by Rahul: 06/07/2020
 *
 *  The MoveGenerator class's goal is to create a generate a list of
 *  squares that a piece on a given square can move to. The generated moves
 *  are not validated for checks and other criterion.
 *
 */

public class MoveGenerator {


    /**
     * Retrieves the list of all possible moves (legal and illegal) that can be made
     * from a square by the piece on that square.
     * @param start the square the piece is starting from
     * @param board the board the game is being played on
     * @return the list of possible squares that the piece can move to
     * @see MoveGenerator#generatePawnMoves(Board, Square)
     */
    public ArrayList<Move> generateMoves(Board board, Square start) {

        Piece p = start.getPiece();
        // pawns are tricky
        if(p.getType() == Type.PAWN) return generatePawnMoves(board, start);

        ArrayList<Move> moves = new ArrayList<>();
        Group g = p.getType().getGroup();

        // map to track if direction given by signature is blocked
        HashMap<TranslationSignature, Boolean> directionBlocked = new HashMap<>();

        for (Translation t : g) {
            if(checkTranslation(board, t, start, directionBlocked)){
                moves.add(new Move(start, board.translateSquare(start ,t)));
            }
        }

        return moves;
    }


    /**
     *
     * @param t the translation being checked
     * @param start the square the piece is starting from
     * @param board the board the game is being played on
     * @param directionBlocked map that tracks whether that move direction
     * has been blocked by a piece
     * @return whether that translation is valid on that board
     * @see TranslationSignature
     */
    public boolean checkTranslation(Board board, Translation t, Square start, HashMap<TranslationSignature, Boolean> directionBlocked){

        // if resulting square after translation is out of bounds
        if(!start.translate(t).inBounds()) return false;

        TranslationSignature signatureKey = new TranslationSignature(t);
        // direction has not been explored yet
        directionBlocked.putIfAbsent(signatureKey, false);

        // if that direction is blocked by piece -- doesn't apply to knights.
        if(directionBlocked.get(signatureKey) == true && start.getPiece().getType() != Type.KNIGHT) return false;

        Square s = board.translateSquare(start, t);
        if (s.isOccupied()) {
            // mark that the direction is blocked
            directionBlocked.replace(signatureKey, true);
            if (s.getPiece().getColor() == start.getPiece().getColor()) {
                return false;
            }
        }
        return true;
    }


    /**
     *
     * @param start the square the pawn is starting on
     * @param board the board the game is being played on
     * @return the list of squares the pawn can move to
     * @see MoveGenerator#checkPawnTranslation(Board, Square,Translation)
     * @see MoveGenerator#getEnPassantMoves(Board, Square)
     */
    public ArrayList<Move> generatePawnMoves(Board board, Square start){
        ArrayList<Move> moves = new ArrayList<>();
        for (Translation t : Type.PAWN.getGroup()) {
            if(checkPawnTranslation(board, start, t)){
                moves.add(new Move(start,board.translateSquare(start, t)));
            }
        }
        // add all en passant moves if possible
        moves.addAll(getEnPassantMoves(board, start));
        return moves;
    }


    /**
     * TODO: MAKE THIS PAWN TRANSLATION CHECKING METHOD EASIER TO UNDERSTAND!
     */

    /**
     *
     * @param board the board the game is being played on
     * @param start the square the pawn is starting from
     * @param t the translation being checked
     * @return whether that translation is valid for the pawn
     */
    public boolean checkPawnTranslation(Board board, Square start, Translation t){
        // if square after translation is out of bounds
        if(!start.translate(t).inBounds()) return false;

        Square end = board.translateSquare(start, t);
        // row pawn is initialized
        int startingRow = start.getPiece().getColor() == Color.WHITE? 1 : 6;
        // correct y signature of given pawn
        int y = start.getPiece().getColor() == Color.WHITE? 1 : -1;

        // not correct direction
        if(t.getY() * y < 0) return false;

        // if 1 square move
        if(t.getY() == 1 * y){
            // if also lateral
            if(t.getX() == 1 || t.getX() == -1){
                if(!end.isOccupied()) return false;
                if(end.getPiece().getColor() == start.getPiece().getColor()) return false;
            }else{
                if(end.isOccupied()) return false;
            }
        }

        // if 2 square move
        if(t.getY() == 2 * y){
            if(start.getRow() != startingRow) return false;
            if(board.getSquare((startingRow + end.getRow())/2, end.getCol()).isOccupied()) return false;
            if(end.isOccupied()) return false;
        }

        return true;
    }



    /**
     *
     * @param start the square which the pawn in question is on
     * @param last the last move on the board
     * @return whether the pawn on the given square can en passant
     */
    public boolean canEnPassant(Square start, Move last){
        // if first move of game
        if(last == null) return false;
        // if last move is not a pawn move
        if(last.getEnd().getPiece().getType() != Type.PAWN) return false;
        // if last pawn move and current pawn move are not adjacent
        if(Math.abs(start.getCol() - last.getStart().getCol()) != 1) return false;

        int row = start.getRow();
        // if pawn in question is white
        if(start.getPiece().getColor() == Color.WHITE){
            // white pawn must be on 5th rank
            if(row != 4) return false;
            // black pawn must have moved 2 squares from 7th to 5th rank
            if(last.getStart().getRow() != 6 || last.getEnd().getRow() != 4) return false;
        }else{
            // black pawn must be on 4th rank
            if(row != 3) return false;
            // white pawn must have moved 2 squares from 2nd to 4th rank
            if(last.getStart().getRow() != 1 || last.getEnd().getRow() != 3) return false;
        }

        // if all conditions passed
        return true;
    }

    /**
     *
     * @param start the square which the pawn is on
     * @param board the board the game is being played on
     * @return the square the pawn can en passant to. Null is returned
     * if the pawn cannot en passant.
     * @see MoveGenerator#canEnPassant(Square, Move)
     */
    public ArrayList<Move> getEnPassantMoves(Board board, Square start){
        ArrayList<Move> enpassantMoves = new ArrayList<>();
        Move last = board.getLog().getLastMove();
        // if can en passant
        if(canEnPassant(start, last)){
            // move on to the same column
            int endCol = last.getEnd().getCol();
            // move between last pawn move start row and end row
            int endRow = (last.getStart().getRow() + last.getEnd().getRow())/2;
            enpassantMoves.add(new Move(start, board.getSquare(endRow, endCol)));
        }
        return enpassantMoves;
    }


}
